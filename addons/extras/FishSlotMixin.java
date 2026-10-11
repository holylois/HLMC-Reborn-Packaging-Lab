package holylois.boombox.mixins;

import holylois.boombox.FishData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inventory, hotbar, chests and backpacks: a thin rarity stroke around a trophy fish or fillet (owner 2026-10-11: no filled
 * background behind it) and its weight in the top-left corner.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class FishSlotMixin {
    @Shadow public abstract void fill(int x0, int y0, int x1, int y1, int color);
    @Shadow public abstract void text(Font font, String text, int x, int y, int color, boolean shadow);
    @Shadow public abstract org.joml.Matrix3x2fStack pose();

    @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"))
    private void holyLoisFishGlow(LivingEntity owner, Level level, ItemStack stack, int x, int y, int seed, CallbackInfo info) {
        String rarity = FishData.rarity(stack);
        int color = rarity.equals("uncommon") ? 0 : FishData.color(rarity);
        if (color == 0) return;
        int rgb = color & 0xFFFFFF, line = 0xD0000000 | rgb, soft = 0x50000000 | rgb;
        // A 1 px frame around the item with clipped corners and soft corner dots, so it reads as a stroke, not a box.
        fill(x + 1, y - 1, x + 15, y, line); fill(x + 1, y + 16, x + 15, y + 17, line);
        fill(x - 1, y + 1, x, y + 15, line); fill(x + 16, y + 1, x + 17, y + 15, line);
        fill(x, y, x + 1, y + 1, soft); fill(x + 15, y, x + 16, y + 1, soft); fill(x, y + 15, x + 1, y + 16, soft); fill(x + 15, y + 15, x + 16, y + 16, soft);
    }

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
