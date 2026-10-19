package holylois.boombox.mixins;

import holylois.boombox.HolyRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Elite boss bar titles in our tier colours too (same palette map as tooltips). */
@Mixin(BossEvent.class)
public abstract class BossNameTierMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true, require = 1)
    private void holyLoisTierBossName(CallbackInfoReturnable<Component> info) {
        if (info.getReturnValue() != null) info.setReturnValue(HolyRarity.recolorMob(info.getReturnValue()));
    }
}
