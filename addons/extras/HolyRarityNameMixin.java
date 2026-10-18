package holylois.boombox.mixins;

import holylois.boombox.HolyRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Item names and chat item links use the Holy Lois tier colour instead of the vanilla rarity colour (HolyRarity). Low priority: callbacks at RETURN run in reverse priority order, so this one runs after Runeforged restyles the name. */
@Mixin(value = ItemStack.class, priority = 500)
public abstract class HolyRarityNameMixin {
    @Inject(method = "getStyledHoverName", at = @At("RETURN"), cancellable = true, require = 1)
    private void holyLoisTierName(CallbackInfoReturnable<Component> info) {
        String tier = HolyRarity.of((ItemStack) (Object) this);
        if (!tier.isEmpty()) info.setReturnValue(HolyRarity.style(info.getReturnValue(), tier));
    }

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 1)
    private void holyLoisTierLink(CallbackInfoReturnable<Component> info) {
        String tier = HolyRarity.of((ItemStack) (Object) this);
        if (!tier.isEmpty()) info.setReturnValue(HolyRarity.style(info.getReturnValue(), tier));
    }
}
