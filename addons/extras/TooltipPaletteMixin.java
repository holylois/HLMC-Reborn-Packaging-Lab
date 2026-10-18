package holylois.boombox.mixins;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameDefinition;
import holylois.boombox.FishData;
import holylois.boombox.HolyRarity;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keep Legendary Tooltips' layout, but the frame speaks our tiers (HolyRarity): border in the tier colour, and the gold spike
 * frame only for Legendary; Mythic and Cursed get our own red and purple frames (assets/holylois/textures/gui/tooltip_frames.png,
 * original art from make-frames.py). Its config gave the spikes to every vanilla rare/epic item, which includes anything enchanted.
 */
@Pseudo
@Mixin(targets="com.anthonyhilyard.legendarytooltips.LegendaryTooltips")
public abstract class TooltipPaletteMixin {
    private static final Identifier BORDERS=Identifier.fromNamespaceAndPath("legendarytooltips","textures/gui/tooltip_borders.png"),
        HOLY=Identifier.fromNamespaceAndPath("holylois","textures/gui/tooltip_frames.png");

    @Inject(method="getDefinitionColors",at=@At("RETURN"),cancellable=true,require=1)
    private static void holyLoisTierBorder(ItemStack stack,int bgStart,int bgEnd,int borderStart,int borderEnd,
                                         HolderLookup.Provider registries,CallbackInfoReturnable<FrameDefinition> info){
        var old=info.getReturnValue();
        if(old==null)return;
        String tier=HolyRarity.of(stack);
        boolean fancy=HolyRarity.rank(tier)>=HolyRarity.rank("legendary"),hadFrame=old.index()>=0;
        int index=fancy?(hadFrame?old.index():0):(hadFrame?-1:old.index());
        int color=FishData.color(tier);
        if(index==old.index()&&color==0)return;
        boolean ours=tier.equals("mythic")||tier.equals("cursed");
        if(ours)index=tier.equals("mythic")?0:1;
        boolean defaults=ours||fancy&&!hadFrame;
        info.setReturnValue(new FrameDefinition(ours?HOLY:old.resource()!=null?old.resource():BORDERS,index,
            color!=0?()->color:old.startBorder(),color!=0?()->color:old.endBorder(),old.startBackground(),old.endBackground(),old.source(),old.priority(),
            defaults?LegendaryTooltipsConfig.DEFAULT_FRAME_WIDTH:old.frameWidth(),defaults?LegendaryTooltipsConfig.DEFAULT_PART_SIZE:old.partSize(),
            defaults?LegendaryTooltipsConfig.DEFAULT_PART_OFFSET:old.partOffset(),defaults?LegendaryTooltipsConfig.DEFAULT_CORNER_OFFSET:old.cornerOffset()));
    }
}
