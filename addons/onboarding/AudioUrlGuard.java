package holylois;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.io.ModalCloseable;

/**
 * AudioPlayer's /audioplayer url (AudioUrlMixin): https only, never to local or internal addresses (also after
 * redirects), one new download per player every 20 seconds, a size cap and timeouts, only real mp3/wav bytes kept, and a log of who added what
 * (world/holylois/audio-urls.log). Operators skip the cooldown only.
 */
public final class AudioUrlGuard {
    private AudioUrlGuard() {}
    private static final Logger LOG = LoggerFactory.getLogger("HolyLois");
    static final long COOLDOWN_MS = 20_000;
    static final int MAX_REDIRECTS = 5;
    private static final Map<UUID, Long> lastDownload = new ConcurrentHashMap<>();
    private static Path logFile;

    static void load(Path worldRoot) { logFile = worldRoot.resolve("holylois/audio-urls.log"); }

    /** Before AudioPlayer reads the address: throws with the message the player sees. */
    public static void check(ServerPlayer player, String address) {
        URI uri = parse(address);
        long now = System.currentTimeMillis();
        boolean op = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        long wait = op ? 0 : waitMs(lastDownload.get(player.getUUID()), now);
        if (wait > 0) throw new IllegalArgumentException("Please wait " + (wait + 999) / 1000 + " s before adding another song.");
        requirePublic(uri.getHost());
        lastDownload.put(player.getUUID(), now);
        record(player, uri.toString());
    }

    /** Replaces AudioPlayer's unbounded download: every redirect is checked again, the body stops at the size limit. */
    public static byte[] download(String address, long maxBytes) throws IOException {
        if (maxBytes <= 0 || maxBytes > Integer.MAX_VALUE) throw new IOException("Invalid audio size limit.");
        URI uri = parse(address);
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            // The connection resolver returns these exact checked bytes. TLS still verifies the original hostname.
            var resolver = pinned(uri.getHost(), InetAddress.getAllByName(uri.getHost()));
            var manager = PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(resolver)
                .setDefaultConnectionConfig(ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(10))
                    .setSocketTimeout(Timeout.ofSeconds(30)).build()).build();
            try (var client = HttpClients.custom().setConnectionManager(manager).disableRedirectHandling()
                    .disableAutomaticRetries().disableCookieManagement().disableContentCompression()
                    .setUserAgent("AudioPlayer").setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(10)).setResponseTimeout(Timeout.ofSeconds(30)).build()).build()) {
            var connection = client.executeOpen(null, new HttpGet(uri), null);
            try {
            int status = connection.getCode();
            if (status >= 300 && status < 400) {
                var header = connection.getFirstHeader("Location");
                String location = header == null ? null : header.getValue();
                if (location == null) throw new IOException("Broken redirect");
                uri = parse(uri.resolve(location).toString());
                continue;
            }
            if (status != 200) throw new IOException("The link answered with HTTP " + status);
            if (connection.getEntity() == null) throw new IOException("That link has no audio file.");
            if (connection.getEntity().getContentLength() > maxBytes) throw new IOException(tooBig(maxBytes));
                InputStream in = connection.getEntity().getContent();
                var out = new ByteArrayOutputStream();
                byte[] buffer = new byte[64 * 1024];
                for (int read; (read = in.read(buffer)) != -1; ) {
                    if (out.size() + read > maxBytes) throw new IOException(tooBig(maxBytes));
                    out.write(buffer, 0, read);
                }
                byte[] body = out.toByteArray();
                if (!isAudio(body)) throw new IOException("That link is not an mp3 or wav file.");
                return body;
            } finally { closeResponse(connection); }
            }
        }
        throw new IOException("Too many redirects");
    }

    static void closeResponse(ClassicHttpResponse response) throws IOException {
        // Graceful close can drain an oversized or redirect body after our size guard rejected it.
        if (response instanceof ModalCloseable modal) modal.close(CloseMode.IMMEDIATE);
        else response.close();
    }

    /** Only real audio is kept (owner 2026-10-11): a WAV header ("RIFF....WAVE"), an ID3 tag or an MP3 frame sync at the start. */
    static boolean isAudio(byte[] body) {
        if (body.length >= 12 && body[0] == 'R' && body[1] == 'I' && body[2] == 'F' && body[3] == 'F'
            && body[8] == 'W' && body[9] == 'A' && body[10] == 'V' && body[11] == 'E') return true;
        if (body.length >= 3 && body[0] == 'I' && body[1] == 'D' && body[2] == '3') return true;
        return body.length >= 2 && (body[0] & 0xFF) == 0xFF && (body[1] & 0xE0) == 0xE0;
    }

    static URI parse(String address) {
        URI uri;
        try { uri = new URI(address.strip()); } catch (Exception error) { throw new IllegalArgumentException("That is not a valid link."); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getPort() == 0 || uri.getPort() > 65535)
            throw new IllegalArgumentException("Only https:// links to an mp3 or wav file work.");
        return uri;
    }

    static long waitMs(Long last, long now) { return last == null ? 0 : Math.max(0, last + COOLDOWN_MS - now); }

    private static void requirePublic(String host) {
        InetAddress[] addresses;
        try { addresses = InetAddress.getAllByName(host); } catch (Exception error) { throw new IllegalArgumentException("That website could not be found."); }
        pinned(host, addresses);
    }

    static DnsResolver pinned(String host, InetAddress[] resolved) {
        InetAddress[] addresses = resolved.clone();
        if (addresses.length == 0) throw new IllegalArgumentException("That website could not be found.");
        for (InetAddress address : addresses)
            if (!isPublic(address)) throw new IllegalArgumentException("That link points to a private address and is blocked.");
        return new DnsResolver() {
            public InetAddress[] resolve(String requested) throws UnknownHostException {
                if (!host.equalsIgnoreCase(requested)) throw new UnknownHostException("Unexpected download host");
                return addresses.clone();
            }
            public String resolveCanonicalHostname(String requested) throws UnknownHostException {
                if (!host.equalsIgnoreCase(requested)) throw new UnknownHostException("Unexpected download host");
                return host;
            }
        };
    }

    /** Query strings, paths and fragments can contain private song links or credentials. Log the origin only. */
    static String logOrigin(URI uri) {
        return "https://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
    }

    /** False for loopback, private, link-local (cloud metadata), carrier-grade NAT, unique-local, multicast and unspecified. */
    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
            || address.isMulticastAddress()) return false;
        byte[] b = address.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xff, second = b[1] & 0xff;
            int third = b[2] & 0xff;
            return first != 0 && !(first == 100 && second >= 64 && second < 128)
                && !(first == 192 && second == 0 && (third == 0 || third == 2))
                && !(first == 192 && second == 88 && third == 99)
                && !(first == 198 && (second == 18 || second == 19 || second == 51 && third == 100))
                && !(first == 203 && second == 0 && third == 113) && first < 224;
        }
        if ((b[0] & 0xfe) == 0xfc) return false; // fc00::/7 unique local
        boolean mapped = true;
        for (int i = 0; i < 10; i++) mapped &= b[i] == 0;
        if (mapped && (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff) {
            try { return isPublic(InetAddress.getByAddress(new byte[] {b[12], b[13], b[14], b[15]})); } catch (Exception error) { return false; }
        }
        // Only ordinary global unicast. Translation/tunnel prefixes can reach private IPv4 through a gateway.
        return (b[0] & 0xe0) == 0x20 && !((b[0] & 0xff) == 0x20 && (b[1] & 0xff) == 0x02)
            && !((b[0] & 0xff) == 0x20 && (b[1] & 0xff) == 0x01 && b[2] == 0 && b[3] == 0)
            && !((b[0] & 0xff) == 0x20 && (b[1] & 0xff) == 0x01 && (b[2] & 0xff) == 0x0d && (b[3] & 0xff) == 0xb8);
    }

    private static String tooBig(long maxBytes) { return "That file is bigger than " + maxBytes / 1_000_000 + " MB."; }

    private static void record(ServerPlayer player, String url) {
        String line = Instant.now() + " " + player.getGameProfile().name() + " " + player.getUUID() + " " + logOrigin(parse(url));
        LOG.info("Holy Lois audio link: {}", line);
        if (logFile == null) return;
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException error) { LOG.warn("Cannot write the audio link log", error); }
    }
}
