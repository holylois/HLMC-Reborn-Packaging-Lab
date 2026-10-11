package holylois.boombox.mixins;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Picking up a fish and clicking the same slot again quickly is a vanilla double-click ("collect all"), which does nothing for
 * an item that never stacks: it stayed on the cursor, drawn right over the slot, and looked placed (owner 2026-10-11, "phantom
 * fish"). With a single-stack item on the cursor the second click is a normal click, so it goes back into the slot.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class SingleItemDoubleClickMixin {
    @Shadow private boolean doubleclick;
    @Shadow @Final protected AbstractContainerMenu menu;

    @Inject(method = "mouseReleased", at = @At("HEAD"), require = 1)
    private void holyLoisPlaceSingleItem(MouseButtonEvent event, CallbackInfoReturnable<Boolean> info) {
        if (!doubleclick || event.hasShiftDown()) return;
        var carried = menu.getCarried();
        if (!carried.isEmpty() && carried.getMaxStackSize() == 1) doubleclick = false;
    }
}
