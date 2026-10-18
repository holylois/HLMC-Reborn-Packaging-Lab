package holylois.boombox;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import holylois.boombox.mixins.RenderPipelinesAccessor;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Rarity stroke (owner 2026-10-11): a 1 pixel outline that follows the item's own shape, only where items are drawn in menus,
 * the hotbar and tooltips, never in hand or on the ground. GUI items are rendered once into an atlas and copied to the screen;
 * the same atlas picture is copied four times first, shifted one pixel left/right/up/down, as a flat silhouette in the rarity
 * colour (shaders/core/rarity_outline.fsh), and the item lands on top. 26.3 sorts a layer's draws by pipeline with a random
 * key, so the item copy uses the same pipeline: equal keys keep their order. Legendary and Mythic get a moving gradient.
 * Every item with a Holy Lois tier (HolyRarity): fish, fillets, crates, gear and loot.
 */
public final class RarityOutline {
    private RarityOutline() {}

    /** Carried by each GUI item render state (RarityOutlineStateMixin). */
    public interface Tagged {
        int holyLoisOutline();
        void holyLoisSetOutline(int argb);
    }

    /** Stroke of the item being extracted right now (set around GuiGraphicsExtractor.item, render thread only). */
    public static int pending;

    private static final int[][] OFFSETS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static RenderPipeline pipeline;

    /** ARGB of the stroke, 0 for none. Alpha 254 = flat colour, 253/252 = the Legendary/Mythic gradient (see the shader). */
    public static int of(ItemStack stack) {
        String rarity = HolyRarity.of(stack);
        if (!HolyRarity.outlined(stack, rarity)) return 0;
        int rgb = FishData.color(rarity) & 0xFFFFFF;
        return switch (rarity) {
            case "uncommon", "rare", "epic", "cursed" -> 0xFE000000 | rgb;
            case "legendary" -> 0xFD000000 | rgb;
            case "mythic" -> 0xFC000000 | rgb;
            default -> 0;
        };
    }

    /** Draws the stroke and the item; false (vanilla draws the item) when it has no stroke. */
    public static boolean submit(GuiRenderState render, GuiItemRenderState item, GuiItemAtlas.SlotView view) {
        int color = ((Tagged) (Object) item).holyLoisOutline();
        if (color == 0) return false;
        if (pipeline == null) pipeline = RenderPipeline.builder(RenderPipelinesAccessor.holyLoisGuiTextured())
            .withLocation(Identifier.fromNamespaceAndPath("holylois", "pipeline/rarity_outline"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("holylois", "core/rarity_outline"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA)).build();
        var setup = TextureSetup.singleTexture(view.textureView(), RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST));
        int x = item.x(), y = item.y();
        for (int[] o : OFFSETS)
            render.addBlitToCurrentLayer(new BlitRenderState(pipeline, setup, item.pose(), x + o[0], y + o[1], x + 16 + o[0], y + 16 + o[1],
                view.u0(), view.u1(), view.v0(), view.v1(), color, item.scissorArea()));
        render.addBlitToCurrentLayer(new BlitRenderState(pipeline, setup, item.pose(), x, y, x + 16, y + 16,
            view.u0(), view.u1(), view.v0(), view.v1(), 0xFFFFFFFF, item.scissorArea()));
        return true;
    }
}
