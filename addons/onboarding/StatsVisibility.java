package holylois;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/** Shared with the website stats generator; game saves remain untouched. */
final class StatsVisibility {
    private static final Path FILE = Path.of("config", "holylois-stats-hidden.json");
    private static final Pattern UNSAFE = Pattern.compile("n[i1!|l][g6q9]{2}|f[a@4][g6q9]{1,2}[o0]t|f[a@4][g6q9]$|r[e3]t[a@4]rd|p[i1!]d[o0a]r|k[i1!]ke|ch[i1!]nk|tr[a@4]nn(?:y|ie)|п[иі]д[оа]р|(?:p[o0]|n[a@4]|n[i1])h[uy][ijy]|x[uy][ijy](?!e)|p[i1!]zd|bl[yj]a[dt]|[yj][e3]b[a@4][nlt]|d[o0]lb[o0][e3]b|m[uy]d[a@4]k|g[a@4]nd[o0]n|z[a@4]l[uy]p|p[i1!]mp[i1!]s|m[a@4][uy]k[a@4]", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final class Config { List<String> names = List.of("pjampjam", "pjamtest", "pjanpjan"); }
    static boolean visible(String name) {
        if (name.equalsIgnoreCase("pjampjam") || name.equalsIgnoreCase("pjamtest") || name.equalsIgnoreCase("pjanpjan")) return false;
        try {
            if (Files.exists(FILE)) {
                var config = new Gson().fromJson(Files.readString(FILE), Config.class);
                if (config != null && config.names != null && config.names.stream().anyMatch(name::equalsIgnoreCase)) return false;
            }
        } catch (Exception error) { org.slf4j.LoggerFactory.getLogger("HolyLois").warn("Could not read stats exclusions", error); }
        return true;
    }
    static String display(String name) { return UNSAFE.matcher(name).find() ? name.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + "*****" : name; }
}
