package holylois.boombox.mixins;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameDefinition;
import holylois.boombox.TierFrames;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The frame Legendary Tooltips actually draws comes from its config lookup, not the colour lookup: same tier rules (TierFrames). */
@Pseudo
@Mixin(targets="com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig")
public abstract class TooltipFrameMixin {
    @Inject(method="getFrameDefinition",at=@At("RETURN"),cancellable=true,require=1)
    private void holyLoisTierFrame(ItemStack stack,HolderLookup.Provider registries,CallbackInfoReturnable<FrameDefinition> info){
        info.setReturnValue(TierFrames.adjust(stack,info.getReturnValue()));
    }
}
