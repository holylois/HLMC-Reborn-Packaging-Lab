package holylois.boombox.mixins;

import holylois.boombox.FishData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inventory, hotbar, chests and backpacks: a trophy fish's weight in the top-left corner. The rarity stroke around the
 * item's own shape is RarityOutline.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class FishSlotMixin {
    @Shadow public abstract void text(Font font, String text, int x, int y, int color, boolean shadow);
    @Shadow public abstract org.joml.Matrix3x2fStack pose();

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("TAIL"))
    private void holyLoisFishWeight(Font font, ItemStack stack, int x, int y, String countText, CallbackInfo info) {
        String label = FishData.label(stack);
        if (label == null) return;
        int color = FishData.color(FishData.rarity(stack));
        var pose = pose();
        pose.pushMatrix();
        pose.translate(x + 0.5f, y + 0.5f);
        pose.scale(0.5f, 0.5f);
        text(font, label, 0, 0, color == 0 ? 0xFFF3F0E8 : color, true);
        pose.popMatrix();
    }
}
