package holylois.boombox.mixins;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameDefinition;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Legendary Tooltips only draws a frame's top and bottom ornament when the tooltip is at least as wide as the ornament, and Iceberg
 * never pads a tooltip that is only its title (Beacon). Only that width check sees the ornament width (restored right after it, so the
 * ornament stays centred and may overhang a short name a little).
 */
@Pseudo
@Mixin(targets = "com.anthonyhilyard.legendarytooltips.tooltip.TooltipDecor")
public abstract class TooltipCrestMixin {
    /** The real width: @ModifyVariable writes its result back into the variable, so the next load restores it for the centring. */
    private static int holyLoisRealWidth = -1;

    @ModifyVariable(method = "drawBorder", at = @At(value = "LOAD", ordinal = 5), index = 3, argsOnly = true, require = 1)
    private static int holyLoisCrestOnShortNames(int width, @Local(argsOnly = true) FrameDefinition frame) {
        holyLoisRealWidth = width;
        return frame == null || frame.index() < 0 ? width : Math.max(width, frame.frameWidth() - 2 * frame.partSize());
    }

    @ModifyVariable(method = "drawBorder", at = @At(value = "LOAD", ordinal = 6), index = 3, argsOnly = true, require = 1)
    private static int holyLoisCrestCentred(int width) {
        int real = holyLoisRealWidth; holyLoisRealWidth = -1;
        return real >= 0 ? real : width;
    }
}
