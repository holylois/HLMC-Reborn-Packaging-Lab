package holylois.boombox.mixins;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Pack look for Legendary Tooltips (owner 2026-10-19): no item picture in the title, so the name sits centred, and a minimum width
 * so a short name (Beacon) still has room for its frame's spikes. Set in code because the shared-defaults merge keeps values that
 * already sit in a player's file.
 */
@Pseudo
@Mixin(LegendaryTooltipsConfig.class)
public abstract class TooltipOptionsMixin {
    @Shadow @Final @Mutable public Supplier<Object> renderItemModel;  // Supplier<ModelRenderType>, a private enum
    @Shadow @Final @Mutable public Supplier<Boolean> enforceMinimumWidth;

    @Inject(method = "<init>", at = @At("RETURN"), require = 1)
    private void holyLoisPackLook(CallbackInfo info) {
        try {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object none = Enum.valueOf((Class) Class.forName("com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig$ModelRenderType"), "NONE");
            renderItemModel = () -> none;
        } catch (ReflectiveOperationException | RuntimeException ignored) {}
        enforceMinimumWidth = () -> true;
    }
}
