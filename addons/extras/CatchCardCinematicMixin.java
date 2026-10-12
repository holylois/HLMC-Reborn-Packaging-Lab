package holylois.boombox.mixins;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3 can skip Hud extraction entirely during the cinematic. Append only our cards to the completed GUI state instead,
 * after letterboxing/chat, independently of the normal HUD visibility. Normal gameplay still uses the HUD element.
 */
@Mixin(Gui.class)
public abstract class CatchCardCinematicMixin {
    @Shadow @Final private GuiRenderState guiRenderState;
    @Inject(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At("TAIL"), require=1)
    private void holyLoisCardsOverCinematic(DeltaTracker delta,boolean hud,boolean overlays,CallbackInfo info) {
        if (holylois.boombox.CinematicTweaks.playing()&&holylois.boombox.CinematicCards.enabled()){
            var g=new GuiGraphicsExtractor(net.minecraft.client.Minecraft.getInstance(),guiRenderState,0,0);
            g.nextStratum();holylois.boombox.RareCatchHud.card(g,true);
        }
    }
}
