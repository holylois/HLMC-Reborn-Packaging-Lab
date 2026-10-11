package holylois.boombox.mixins;

import holylois.boombox.RarityOutline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rarity stroke wiring (see RarityOutline): remember the stroke while an item is extracted, keep it on its render state, draw it. */
public final class RarityOutlineMixin {
    private RarityOutlineMixin() {}

    @Mixin(GuiGraphicsExtractor.class)
    public abstract static class Extract {
        @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
        private void holyLoisOutlineStart(LivingEntity owner, Level level, ItemStack stack, int x, int y, int seed, CallbackInfo info) {
            RarityOutline.pending = RarityOutline.of(stack);
        }

        @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("RETURN"))
        private void holyLoisOutlineEnd(LivingEntity owner, Level level, ItemStack stack, int x, int y, int seed, CallbackInfo info) {
            RarityOutline.pending = 0;
        }
    }

    @Mixin(GuiItemRenderState.class)
    public abstract static class State implements RarityOutline.Tagged {
        @Unique private int holyLoisOutline;

        @Inject(method = "<init>", at = @At("TAIL"))
        private void holyLoisTag(Matrix3x2f pose, TrackingItemStackRenderState item, int x, int y, ScreenRectangle scissor, CallbackInfo info) {
            holyLoisOutline = RarityOutline.pending;
        }

        @Override public int holyLoisOutline() { return holyLoisOutline; }
        @Override public void holyLoisSetOutline(int argb) { holyLoisOutline = argb; }
    }

    @Mixin(GuiRenderer.class)
    public abstract static class Draw {
        @Shadow @Final private GuiRenderState renderState;

        @Inject(method = "submitBlitFromItemAtlas", at = @At("HEAD"), cancellable = true)
        private void holyLoisOutline(GuiItemRenderState item, GuiItemAtlas.SlotView view, CallbackInfo info) {
            if (RarityOutline.submit(renderState, item, view)) info.cancel();
        }
    }
}
