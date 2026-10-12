package holylois.boombox.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import holylois.boombox.ShineText;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Per-glyph colour and wave for text in a shine font (Legendary and Mythic names), see ShineText. */
@Mixin(targets = "net.minecraft.client.gui.Font$PreparedTextBuilder")
public abstract class ShineTextMixin implements ShineText.Layout {
    @Shadow private float x;
    @Unique private float holyLoisOrigin;
    @Unique private java.util.List<ShineText.Run> holyLoisRuns=java.util.List.of();
    @Override public void holyLoisRuns(java.util.List<ShineText.Run> runs){holyLoisOrigin=x;holyLoisRuns=runs;}

    @ModifyExpressionValue(method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font$PreparedTextBuilder;getTextColor(Lnet/minecraft/network/chat/TextColor;)I"))
    private int holyLoisShine(int color, @Local(argsOnly = true) Style style) {
        float relative=x-holyLoisOrigin;
        for(var run:holyLoisRuns)if(relative>=run.start()-0.01f&&relative<run.end()+0.01f&&ShineText.kind(style)==run.kind())
            return ShineText.color(style,relative-run.start(),run.end()-run.start(),color);
        return ShineText.color(style, relative, color);
    }

    @ModifyArg(method = "accept(ILnet/minecraft/network/chat/Style;Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;createGlyph(FFIILnet/minecraft/network/chat/Style;FF)Lnet/minecraft/client/gui/font/TextRenderable$Styled;"),
        index = 1)
    private float holyLoisWave(float y, @Local(argsOnly = true) Style style) {
        return y + ShineText.wave(style, x);
    }
}
