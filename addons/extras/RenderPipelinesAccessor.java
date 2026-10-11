package holylois.boombox.mixins;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The GUI textured snippet, so the rarity stroke pipeline shares vanilla's GUI setup (RarityOutline). */
@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
    @Accessor("GUI_TEXTURED_SNIPPET")
    static RenderPipeline.Snippet holyLoisGuiTextured() { throw new AssertionError(); }
}
