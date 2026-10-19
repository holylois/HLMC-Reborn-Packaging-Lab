package holylois.boombox;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;

/**
 * Iris 26.3 workaround (was the separate holylois-glint-preload jar): load the vanilla glint textures at tick start, outside
 * a render pass, so Iris never uploads them mid-pass. Two map lookups per tick once loaded; a resource reload re-creates them.
 */
public final class GlintPreload implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) return;
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!client.isGameLoadFinished()) return;
            var textures = client.getTextureManager();
            textures.getTexture(ItemFeatureRenderer.ENCHANTED_GLINT_ITEM);
            textures.getTexture(ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR);
        });
    }
}
