package holylois.boombox.mixins;

import holylois.boombox.CrateStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use the same crate tier for the held name, tooltip title, and loot card. */
@Mixin(ItemStack.class)
public abstract class CrateNameMixin {
    @Inject(method="getStyledHoverName",at=@At("RETURN"),cancellable=true,require=1)
    private void holyLoisCrateName(CallbackInfoReturnable<Component> info){
        var stack=(ItemStack)(Object)this;
        if(!CrateStyle.rarity(stack).isEmpty())info.setReturnValue(CrateStyle.name(stack));
    }
}
