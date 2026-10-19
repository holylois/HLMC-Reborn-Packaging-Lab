package holylois.boombox.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import holylois.boombox.RelicLooks;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Relics look like themselves (RelicLooks): client side only, so relics already in chests and inventories need no migration. */
@Mixin(ItemModelResolver.class)
public abstract class RelicModelMixin {
    @WrapOperation(method = {"appendItemLayers", "shouldPlaySwapAnimation", "swapAnimationScale"}, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"), require = 3)
    private Object holyLoisRelicModel(ItemStack stack, DataComponentType<?> type, Operation<Object> original) {
        if (type == DataComponents.ITEM_MODEL) { var look = RelicLooks.model(stack); if (look != null) return look; }
        return original.call(stack, type);
    }
}
