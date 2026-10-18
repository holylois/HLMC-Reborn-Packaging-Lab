package holylois;

import com.google.gson.Gson;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * The server owner can be on without announcements: broadcasts about the names in config/holylois-quiet.json (joins, leaves, AFK,
 * advancements, discoveries) are not sent to anybody. Deaths are the one exception, so friends still see them in the chat.
 * The Discord bot reads the same file. Typed chat is not touched. The file is re-read when it changes.
 */
final class Quiet {
    private static final Logger LOG = LoggerFactory.getLogger("HolyLois");
    private static final Path FILE = Path.of("config", "holylois-quiet.json");
    static final class Config { public List<String> names = new ArrayList<>(List.of("pjampjam", "pjamtest", "pjanpjan")); }

    private static long stamp = Long.MIN_VALUE;
    private static List<Pattern> hidden = List.of();
    private static List<String> plain = List.of();
    /** Lowercase quiet names that chose to be seen with /hidden off; cleared by a restart (hidden is the default). */
    static final Set<String> SHOWN = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private Quiet() {}

    private static synchronized List<Pattern> names() {
        try {
            if (!Files.exists(FILE)) {
                Files.createDirectories(FILE.getParent());
                Files.writeString(FILE, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(new Config()));
            }
            long modified = Files.getLastModifiedTime(FILE).toMillis();
            if (modified != stamp) {
                var config = new Gson().fromJson(Files.readString(FILE), Config.class);
                plain = config == null || config.names == null ? List.of() : config.names.stream().filter(n -> !n.isBlank()).map(String::strip).toList();
                hidden = plain.stream().map(Quiet::pattern).toList();
                stamp = modified;
                LOG.info("Holy Lois quiet names: {}", hidden.size());
            }
        } catch (Exception error) { LOG.warn("Keeping the last quiet names", error); }
        return hidden;
    }

    private static synchronized String plainName(int index) { return index < plain.size() ? plain.get(index).toLowerCase(Locale.ROOT) : ""; }

    static Pattern pattern(String name) { return Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name.strip()) + "(?![A-Za-z0-9_])", Pattern.CASE_INSENSITIVE); }

    /** True when NAME is one of the quiet names (whole name, any case) and has not chosen /hidden off. */
    static boolean quiet(String name) {
        return listed(name) && !SHOWN.contains(name.toLowerCase(Locale.ROOT));
    }

    /** True when NAME is in the quiet file, whatever its current /hidden choice. */
    static boolean listed(String name) {
        if (name == null) return false;
        for (var pattern : names()) if (pattern.matcher(name).matches()) return true;
        return false;
    }

    /** False when this broadcast is about a quiet player and is not a death message. */
    static boolean allow(Component message) { return allow(message.getString(), message.getContents() instanceof TranslatableContents t ? t.getKey() : ""); }

    static boolean allow(String text, String translationKey) {
        if (translationKey.startsWith("death.")) return true;
        var patterns = names();
        for (int i = 0; i < patterns.size(); i++)
            if (!SHOWN.contains(plainName(i)) && patterns.get(i).matcher(text).find()) return false;
        return true;
    }
}
