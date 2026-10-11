package holylois.boombox.mixins;

import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bite frames count use ticks against a normal 1.6 s meal. A trophy fish that takes longer to eat (FishEatTimeMixin) reports
 * its progress on that same scale, so the bites spread over the whole meal instead of ending in the first second.
 */
@Mixin(UseDuration.class)
public abstract class FishEatFramesMixin {
    @Inject(method = "useDuration", at = @At("RETURN"), cancellable = true, require = 1)
    private static void holyLoisEvenBites(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> info) {
        int total = stack.getUseDuration(user);
        if (total > 32 && holylois.boombox.FishData.weighed(stack)) info.setReturnValue(Math.round(info.getReturnValueI() * 32f / total));
    }
}
