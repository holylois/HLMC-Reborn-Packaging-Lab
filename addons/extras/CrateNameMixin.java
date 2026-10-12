package holylois.boombox.mixins;

import holylois.boombox.CrateStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** Use the same crate tier for the held name, tooltip title, and loot card. */
@Mixin(ItemStack.class)
public abstract class CrateNameMixin {
    @WrapMethod(method="getStyledHoverName")
    private Component holyLoisCrateName(Operation<Component> original){
        var stack=(ItemStack)(Object)this;
        var name=original.call();
        if(holylois.boombox.TreasureMapTooltip.matches(stack))return holylois.boombox.TreasureMapTooltip.name();
        return CrateStyle.rarity(stack).isEmpty()?name:CrateStyle.name(stack);
    }
}
