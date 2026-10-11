package holylois.boombox.mixins;

import holylois.boombox.FishData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rarer trophy fish take longer to eat (FishData.eatTicks), on the server and in the client's prediction alike. */
@Mixin(ItemStack.class)
public abstract class FishEatTimeMixin {
    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true, require = 1)
    private void holyLoisFishEatTime(LivingEntity user, CallbackInfoReturnable<Integer> info) {
        int normal = info.getReturnValueI();
        if (normal > 0) info.setReturnValue(FishData.eatTicks((ItemStack) (Object) this, normal));
    }
}
