package holylois.boombox.mixins;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ji AFK Cinematic cancels the whole HUD while its camera runs. Priority 500 puts this before its cancel, so a catch card (and an
 * item card) still shows over the cinematic; only then, the HUD draws them itself otherwise.
 */
@Mixin(value = Gui.class, priority = 500)
public abstract class CatchCardCinematicMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"))
    private void holyLoisCardsOverCinematic(GuiGraphicsExtractor g, DeltaTracker delta, CallbackInfo info) {
        if (holylois.boombox.CinematicTweaks.playing()) holylois.boombox.RareCatchHud.card(g, true);
    }
}
