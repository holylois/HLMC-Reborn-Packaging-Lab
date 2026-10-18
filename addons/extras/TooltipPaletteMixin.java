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

/** Keep Legendary Tooltips' layout, but its colour lookup speaks our tiers too (TierFrames; the drawn frame: TooltipFrameMixin). */
@Pseudo
@Mixin(targets="com.anthonyhilyard.legendarytooltips.LegendaryTooltips")
public abstract class TooltipPaletteMixin {
    @Inject(method="getDefinitionColors",at=@At("RETURN"),cancellable=true,require=1)
    private static void holyLoisTierBorder(ItemStack stack,int bgStart,int bgEnd,int borderStart,int borderEnd,
                                         HolderLookup.Provider registries,CallbackInfoReturnable<FrameDefinition> info){
        info.setReturnValue(TierFrames.adjust(stack,info.getReturnValue()));
    }
}
