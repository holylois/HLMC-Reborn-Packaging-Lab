package holylois.boombox.mixins;

import holylois.boombox.HolyRarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Runeforged's floating elite name tags (text displays with level and affixes) take every shade from one base colour: make
 * that base our tier colour, so magic elites are Uncommon green, legendary gold and Cursed elites Mythic red. Plain colours,
 * no shine. Server side, where the tags are built.
 */
@Pseudo
@Mixin(targets = "com.ultimatearpg.monster.ArpgEliteMonsterService")
public abstract class EliteColorMixin {
    @Inject(method = "eliteColor", at = @At("RETURN"), cancellable = true, require = 1)
    private static void holyLoisEliteTier(int affixes, CallbackInfoReturnable<Integer> info) {
        info.setReturnValue(HolyRarity.remap(info.getReturnValue()));  // Cursed elites come as Runeforged red: Mythic
    }
}
