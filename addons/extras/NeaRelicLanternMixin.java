package holylois.boombox.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import holylois.boombox.RelicLooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Not Enough Animations hangs a held lantern from the hand by drawing the lantern block itself, so Lois's Lantern looked like a
 * plain lantern there. In the same pose we draw the relic's own 3D model (block space, so it lines up with the vanilla one).
 */
@Pseudo
@Mixin(targets = "dev.tr7zw.notenoughanimations.logic.HeldItemHandler", remap = false)
public abstract class NeaRelicLanternMixin {
    @WrapOperation(method = "lanternAnimation", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/block/BlockModelRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"), require = 0)
    private void holyLoisRelicLantern(BlockModelRenderState block, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline,
                                      Operation<Void> original, @Local(argsOnly = true) ItemStack stack) {
        if (RelicLooks.model(stack) == null) { original.call(block, pose, collector, light, overlay, outline); return; }
        var mc = Minecraft.getInstance();
        var item = new ItemStackRenderState();
        mc.getItemModelResolver().updateForTopItem(item, stack, ItemDisplayContext.NONE, mc.level, null, 0);
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);   // item models are drawn around their centre, block models from their corner
        item.submit(pose, collector, light, overlay, outline);
        pose.popPose();
    }
}
