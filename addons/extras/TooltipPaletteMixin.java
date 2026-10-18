package holylois.boombox.mixins;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameDefinition;
import holylois.boombox.CrateStyle;
import holylois.boombox.FishData;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep Legendary Tooltips' layout, but use the same tier colors as our catch cards and names. */
@Pseudo
@Mixin(targets="com.anthonyhilyard.legendarytooltips.LegendaryTooltips")
public abstract class TooltipPaletteMixin {
    @Inject(method="getDefinitionColors",at=@At("RETURN"),cancellable=true,require=1)
    private static void holyLoisTierBorder(ItemStack stack,int bgStart,int bgEnd,int borderStart,int borderEnd,
                                         HolderLookup.Provider registries,CallbackInfoReturnable<FrameDefinition> info){
        String tier=holylois.boombox.HolyRarity.of(stack);
        if(tier.isEmpty()||info.getReturnValue()==null)return;
        int color=FishData.color(tier);
        if(color==0)return;
        var old=info.getReturnValue();
        info.setReturnValue(new FrameDefinition(old.resource(),old.index(),()->color,()->color,
            old.startBackground(),old.endBackground(),old.source(),old.priority(),old.frameWidth(),old.partSize(),old.partOffset(),old.cornerOffset()));
    }
}
