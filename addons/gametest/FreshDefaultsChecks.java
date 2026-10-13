package holylois.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.resources.Identifier;

/** Run with a new HOLYLOIS_TEST_PROFILE seeded from the install, before a second launch can repair it. */
final class FreshDefaultsChecks {
    static void run(ClientGameTestContext context, TestSingleplayerContext world) {
        if (System.getenv("HOLYLOIS_TEST_PROFILE") == null) {
            org.slf4j.LoggerFactory.getLogger("HolyLoisTest").info("Fresh defaults diagnostic skipped: choose an isolated HOLYLOIS_TEST_PROFILE seeded from a fresh install.");
            return;
        }
        context.runOnClient(client -> {
            var log = org.slf4j.LoggerFactory.getLogger("HolyLoisTest");
            var selected = client.getResourcePackRepository().getSelectedIds();
            log.info("Fresh selected resource packs: {}", selected);
            for (String name : java.util.List.of("EatingAnimation-1.0.1.zip", "EatingAnimation-FarmersDelight-1.20-26.x.zip",
                "FA+Objects-v2.1.2.zip", "FA+Player-v1.1.zip", "FreshAnimations_v1.10.5.zip", "HolyLois-FishEating.zip", "JustExpressions_v1.2.1.zip"))
                require(selected.contains("file/" + name), "Missing selected pack " + name);
            String player = client.getResourceManager().getResource(Identifier.parse("minecraft:emf/cem/player.jem")).orElseThrow().sourcePackId();
            log.info("Effective player model resource: {}", player);
            require(player.equals("file/FA+Player-v1.1.zip"), "Player animations overridden by " + player);
            require(client.getResourceManager().getResource(Identifier.parse("minecraft:optifine/cem/zombie.jem")).isPresent(), "Missing mob animation model");
            var rei = me.shedaniel.rei.api.client.config.ConfigObject.getInstance();
            log.info("Fresh REI hideIdle={}, scroll={}, favorites={}, tags={}", rei.isHidingEntryPanelIfIdle(), rei.isEntryListWidgetScrolled(), rei.isFavoritesEnabled(), rei.getHiddenCategories());
            require(rei.isHidingEntryPanelIfIdle() && rei.isEntryListWidgetScrolled() && !rei.isFavoritesEnabled(), "REI first-launch defaults not applied");
            require(rei.getHiddenCategories().stream().anyMatch(id -> id.toString().equals("minecraft:plugins/tag")), "Developer tag category not hidden");
            require(client.options.fov().get() == 90, "Fresh FOV is " + client.options.fov().get());
            client.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player));
        });
        context.waitTicks(5);
        context.takeScreenshot("fresh-defaults-inventory");
        context.runOnClient(client -> client.gui.setScreen(new net.minecraft.client.gui.screens.packs.PackSelectionScreen(
            client.getResourcePackRepository(), repository -> client.gui.setScreen(null),
            client.gameDirectory.toPath().resolve("resourcepacks"), net.minecraft.network.chat.Component.literal("Resource packs"))));
        context.waitTicks(5);
        context.takeScreenshot("fresh-defaults-selected-packs");
        context.runOnClient(client -> client.gui.setScreen(null));
    }
    private static void require(boolean result, String message) { if (!result) throw new AssertionError(message); }
}
