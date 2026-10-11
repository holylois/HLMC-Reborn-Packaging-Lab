package holylois.boombox.mixins;

import holylois.boombox.ShaderPacks;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Draws each shader pack as a short name with its strain (see ShaderPacks). Only the text changes. */
@Pseudo
@Mixin(targets = "net.irisshaders.iris.gui.element.ShaderPackSelectionList$ShaderPackEntry", remap = false)
public abstract class ShaderPackNamesMixin {
    @Shadow private String packName;

    @ModifyArg(method = "extractContent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"), index = 1, require = 0)
    private Component holyLoisPackName(Component original) {
        return ShaderPacks.display(packName, original);
    }
}
