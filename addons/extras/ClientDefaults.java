package holylois.boombox;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.nio.file.Path;

/**
 * One-time client setting changes that the launcher cannot merge (REI keeps a JSON5 file with comments).
 * Each change runs before any mod reads its config, only once per player, and only while the player still has
 * REI's original value, so a choice made in REI's own settings is never overwritten.
 */
public final class ClientDefaults implements PreLaunchEntrypoint {
    @Override public void onPreLaunch() {
        var loader = FabricLoader.getInstance();
        if (loader.getEnvironmentType() != EnvType.CLIENT) return;
        apply(loader.getGameDir(), loader.getConfigDir());
    }

    static void apply(Path game, Path config) {
        var marker = config.resolve("holylois-extras-defaults.txt");
        try {
            var done = new LinkedHashSet<>(Files.exists(marker) ? Files.readAllLines(marker) : java.util.List.of());
            var rei = config.resolve("roughlyenoughitems/config.json5");
            boolean fresh = !Files.exists(marker) && !Files.exists(rei);
            if (!Files.exists(rei)) {
                // REI creates its own defaults later, so seed only our four choices before its first read.
                Files.createDirectories(rei.getParent());
                Files.writeString(rei, """
                    {
                      "basics": { "favoritesEnabled": false },
                      "appearance": { "hideEntryPanelIfIdle": true, "scrollingEntryListWidget": true },
                      "advanced": { "miscellaneous": { "categorySettings": { "hiddenCategories": ["minecraft:plugins/tag"] } } }
                    }
                    """, StandardCharsets.UTF_8);
            }
            if (Files.exists(rei)) {
                String text = Files.readString(rei, StandardCharsets.UTF_8), before = text;
                // The item list on the right appears only while searching.
                if (done.add("rei-hide-idle")) text = setting(text, "hideEntryPanelIfIdle", "false", "true");
                // The developer "Tags" tab is hidden from recipe views.
                if (done.add("rei-hide-tags")) text = setting(text, "hiddenCategories", "\\[\\s*\\]", "[\"minecraft:plugins/tag\"]");
                // A scrolling list instead of pages: no arrows and no "0/1" counter, only the search bar.
                if (done.add("rei-scrolling")) text = setting(text, "scrollingEntryListWidget", "false", "true");
                // Keep saved favorites intact, but hide the favorite button/panel as requested.
                if (done.add("rei-no-favorites")) text = setting(text, "favoritesEnabled", "true", "false");
                if (!text.equals(before)) Files.writeString(rei, text, StandardCharsets.UTF_8);
            }
            // Alt-tab keeps the game running instead of opening the pause menu (F3+P turns pausing back on). Only once options.txt
            // exists: creating it here would stop YOSBR from copying the shipped defaults for a new player.
            var options = game.resolve("options.txt");
            // The new-install FOV changes from the shipped 95 to 90. Established personal FOVs stay untouched.
            if (fresh) for (var initialOptions : java.util.List.of(options, config.resolve("yosbr/options.txt"))) {
                if (!Files.exists(initialOptions)) continue;
                String text = Files.readString(initialOptions, StandardCharsets.UTF_8);
                Files.writeString(initialOptions, text.replaceAll("(?m)^fov:0\\.625\\r?$", "fov:0.5"), StandardCharsets.UTF_8);
            }
            repairResourcePacks(game, config, done);
            if (!done.contains("no-pause-on-alt-tab") && Files.exists(options)) {
                String text = Files.readString(options, StandardCharsets.UTF_8);
                String next = text.contains("pauseOnLostFocus:") ? text.replace("pauseOnLostFocus:true", "pauseOnLostFocus:false")
                    : text + (text.endsWith(System.lineSeparator()) || text.endsWith("\n") ? "" : System.lineSeparator()) + "pauseOnLostFocus:false" + System.lineSeparator();
                if (!next.equals(text)) Files.writeString(options, next, StandardCharsets.UTF_8);
                done.add("no-pause-on-alt-tab");
            }
            // Ji AFK Cinematic starts with the server: when Essential Commands marks you AFK (15 min) or a few seconds after a
            // cast, not after 30 s of standing still. Only while the file still has the mod's own 30 s / 10 s.
            var cinematic = config.resolve("ji-afk-cinematic.json");
            if (!done.contains("afk-cinematic-timing") && Files.exists(cinematic)) {
                String text = Files.readString(cinematic, StandardCharsets.UTF_8);
                String next = text.replace("\"afkThresholdSeconds\": 30,", "\"afkThresholdSeconds\": 900,")
                    .replace("\"fishingCinematicThresholdSeconds\": 10,", "\"fishingCinematicThresholdSeconds\": 5,");
                if (!next.equals(text)) Files.writeString(cinematic, next, StandardCharsets.UTF_8);
                done.add("afk-cinematic-timing");
            }
            Files.write(marker, done);
        } catch (Exception error) {
            org.slf4j.LoggerFactory.getLogger("HolyLoisExtras").warn("Could not apply Holy Lois client defaults", error);
        }
    }

    private static String setting(String text, String key, String oldValue, String next) {
        return text.replaceAll("(\\\"" + key + "\\\"\\s*:\\s*)" + oldValue + "(?=\\s*[,}])", "$1" + next);
    }

    private static void repairResourcePacks(Path game, Path config, java.util.Set<String> done) throws java.io.IOException {
        if (done.contains("animation-pack-order-v2")) return;
        var file = config.resolve("resourcepackoverrides.json");
        if (!Files.exists(file)) return;
        var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        var json = com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        var overrides = json.getAsJsonObject("pack_overrides");
        var order = json.getAsJsonArray("default_packs");
        if (overrides == null || order == null) return;
        var bundled = new java.util.ArrayList<String>();
        for (var id : order) {
            String name = id.getAsString();
            if (!java.util.Set.of("file/EatingAnimation-1.0.1.zip", "file/EatingAnimation-FarmersDelight-1.20-26.x.zip",
                "file/HolyLois-FishEating.zip", "file/FreshAnimations_v1.10.5.zip", "file/FA+Objects-v2.1.2.zip",
                "file/JustExpressions_v1.2.1.zip", "file/FA+Player-v1.1.zip").contains(name)
                || !Files.isRegularFile(game.resolve("resourcepacks").resolve(name.substring(5)))) continue;
            var pack = overrides.getAsJsonObject(name);
            if (pack == null || !pack.has("required") || !pack.get("required").getAsBoolean()) continue;
            // Fixed packs are excluded from options.txt in 26.3 and reinserted alphabetically on reload.
            pack.addProperty("fixed_position", false);
            pack.addProperty("hidden", false);
            pack.addProperty("force_compatible", true);
            bundled.add(name);
        }
        if (bundled.isEmpty()) return;
        Files.writeString(file, gson.toJson(json) + "\n", StandardCharsets.UTF_8);
        for (var options : java.util.List.of(game.resolve("options.txt"), config.resolve("yosbr/options.txt"))) {
            if (!Files.exists(options)) continue;
            String text = Files.readString(options);
            var match = java.util.regex.Pattern.compile("(?m)^resourcePacks:(.*)$").matcher(text);
            if (!match.find()) continue;
            var saved = com.google.gson.JsonParser.parseString(match.group(1).strip()).getAsJsonArray();
            var selected = new com.google.gson.JsonArray();
            boolean placed = false;
            for (var id : saved) {
                String name = id.getAsString();
                if (bundled.contains(name)) {
                    if (!placed) { bundled.forEach(selected::add); placed = true; }
                } else selected.add(id);
            }
            if (!placed) bundled.forEach(selected::add);
            String next = match.replaceFirst(java.util.regex.Matcher.quoteReplacement("resourcePacks:" + selected));
            Files.writeString(options, next, StandardCharsets.UTF_8);
        }
        done.add("animation-pack-order-v2");
    }
}
