package holylois;

import java.net.InetAddress;
import java.net.URI;

public final class AudioUrlSecurityTest {
    private static int checked;
    public static void main(String[] args) throws Exception {
        var publicIp = InetAddress.getByName("1.1.1.1");
        var input = new InetAddress[]{publicIp};
        var resolver = AudioUrlGuard.pinned("songs.example", input);
        input[0] = InetAddress.getByName("127.0.0.1");
        check(resolver.resolve("songs.example")[0].equals(publicIp), "DNS input cannot change after validation");
        var returned = resolver.resolve("songs.example"); returned[0] = input[0];
        check(resolver.resolve("SONGS.EXAMPLE")[0].equals(publicIp), "Connection lookup returns checked immutable addresses");
        try { resolver.resolve("other.example"); throw new AssertionError("Unexpected host passed"); }
        catch (java.net.UnknownHostException expected) { checked++; }
        for (String address : new String[]{"127.0.0.1", "169.254.169.254", "100.64.0.1", "::ffff:10.0.0.1",
                "64:ff9b::a00:1", "2002:a00:1::", "2001:db8::1", "192.0.2.1", "198.51.100.1", "203.0.113.1"}) {
            try { AudioUrlGuard.pinned("songs.example", new InetAddress[]{publicIp, InetAddress.getByName(address)});
                throw new AssertionError("Private/special address passed: " + address); }
            catch (IllegalArgumentException expected) { checked++; }
        }
        check(AudioUrlGuard.logOrigin(URI.create("https://songs.example:8443/private/song.mp3?token=secret#private"))
            .equals("https://songs.example:8443"), "Only origin is logged, never URL paths, signed queries or fragments");
        check(AudioUrlGuard.isPublic(InetAddress.getByName("2606:4700::1111")), "Ordinary public IPv6 works");
        var response = new AbortOnlyResponse(); AudioUrlGuard.closeResponse(response);
        check(response.mode == org.apache.hc.core5.io.CloseMode.IMMEDIATE, "Rejected and redirect bodies are aborted without draining unbounded data");
        System.out.println("Passed " + checked + " pinned DNS and audio URL privacy checks");
    }
    private static final class AbortOnlyResponse extends org.apache.hc.core5.http.message.BasicClassicHttpResponse implements org.apache.hc.core5.io.ModalCloseable {
        org.apache.hc.core5.io.CloseMode mode;
        AbortOnlyResponse() { super(302); }
        @Override public void close(org.apache.hc.core5.io.CloseMode value) { mode = value; }
        @Override public void close() { throw new AssertionError("Graceful response close could drain a rejected body"); }
    }
    private static void check(boolean ok, String message) { checked++; if (!ok) throw new AssertionError(message); }
}
