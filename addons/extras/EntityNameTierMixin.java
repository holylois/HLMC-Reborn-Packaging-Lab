package holylois.boombox.mixins;

import holylois.boombox.HolyRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Elite mob nameplates in our tier colours: Runeforged's quality colours (legendary orange becomes gold, Cursed elites Mythic red). */
@Mixin(Entity.class)
public abstract class EntityNameTierMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 1)
    private void holyLoisTierNameplate(CallbackInfoReturnable<Component> info) {
        var name = info.getReturnValue();
        if (name != null && ((Entity) (Object) this).hasCustomName()) info.setReturnValue(HolyRarity.recolorMob(name));
    }
}
