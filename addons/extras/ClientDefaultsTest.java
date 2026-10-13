package holylois.boombox;

import java.nio.file.*;

public final class ClientDefaultsTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        var game=Files.createTempDirectory(Path.of("."), "fresh-defaults-");
        var config=Files.createDirectories(game.resolve("config"));
        Files.writeString(game.resolve("options.txt"), "fov:0.625\nkey_key.jump:key.keyboard.space\nresourcePacks:[\"vanilla\",\"file/Personal.zip\"]\n");
        Files.createDirectories(game.resolve("resourcepacks"));
        for(String name : java.util.List.of("JustExpressions_v1.2.1.zip", "FA+Player-v1.1.zip")) Files.writeString(game.resolve("resourcepacks").resolve(name), "fixture");
        Files.writeString(config.resolve("resourcepackoverrides.json"), """
            {"default_packs":["vanilla","file/JustExpressions_v1.2.1.zip","file/FA+Player-v1.1.zip"],
             "pack_overrides":{"file/JustExpressions_v1.2.1.zip":{"required":true,"hidden":true,"fixed_position":true},
             "file/FA+Player-v1.1.zip":{"required":true,"hidden":true,"fixed_position":true}}}
            """);
        ClientDefaults.apply(game,config);
        var rei=config.resolve("roughlyenoughitems/config.json5");
        var seed=com.google.gson.JsonParser.parseString(Files.readString(rei)).getAsJsonObject();
        check(!seed.getAsJsonObject("basics").get("favoritesEnabled").getAsBoolean(), "First launch hides favorites");
        check(seed.getAsJsonObject("appearance").get("hideEntryPanelIfIdle").getAsBoolean(), "First launch hides idle entries");
        check(seed.getAsJsonObject("appearance").get("scrollingEntryListWidget").getAsBoolean(), "First launch scrolls entries");
        String options=Files.readString(game.resolve("options.txt"));
        check(options.contains("fov:0.5") && options.contains("key_key.jump:key.keyboard.space"), "Fresh 90 FOV keeps keys");
        check(options.indexOf("JustExpressions") < options.indexOf("FA+Player") && options.contains("Personal.zip"), "Repair uses bundled order and retains custom pack");
        var rpo=com.google.gson.JsonParser.parseString(Files.readString(config.resolve("resourcepackoverrides.json"))).getAsJsonObject().getAsJsonObject("pack_overrides").getAsJsonObject("file/FA+Player-v1.1.zip");
        check(!rpo.get("fixed_position").getAsBoolean() && !rpo.get("hidden").getAsBoolean() && rpo.get("force_compatible").getAsBoolean(), "Visible pack order survives saving");
        Files.writeString(rei, "{\"basics\":{\"favoritesEnabled\":true}}");
        Files.writeString(game.resolve("options.txt"), options.replace("fov:0.5", "fov:0.875"));
        ClientDefaults.apply(game,config);
        check(Files.readString(rei).contains("true") && Files.readString(game.resolve("options.txt")).contains("fov:0.875"), "Later player choices stay personal");
        var old=Files.createTempDirectory(Path.of("."), "old-defaults-");
        var oldConfig=Files.createDirectories(old.resolve("config/roughlyenoughitems")).getParent();
        Files.writeString(old.resolve("options.txt"), "fov:0.625\n");
        Files.writeString(oldConfig.resolve("roughlyenoughitems/config.json5"), """
            { "favoritesEnabled" : true, "hideEntryPanelIfIdle":false,
              "scrollingEntryListWidget" : false, "hiddenCategories": [ ], "custom": 77 }
            """);
        ClientDefaults.apply(old,oldConfig);
        String migrated=Files.readString(oldConfig.resolve("roughlyenoughitems/config.json5"));
        check(migrated.contains("\"favoritesEnabled\" : false") && migrated.contains("\"hideEntryPanelIfIdle\":true") && migrated.contains("\"custom\": 77"), "Legacy formatting and unrelated fields retained");
        check(Files.readString(old.resolve("options.txt")).contains("fov:0.625"), "Established FOV unchanged");
        System.out.println("First-launch defaults and personal-choice regressions passed.");
    }
}
