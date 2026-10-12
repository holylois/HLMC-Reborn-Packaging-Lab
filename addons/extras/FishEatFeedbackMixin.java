package holylois.boombox.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import holylois.boombox.FishData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Consumable still measures a 32-tick meal when our fish needs longer. Use elapsed time for its feedback. */
@Mixin(ItemStack.class)
public abstract class FishEatFeedbackMixin {
    @WrapOperation(method="onUseTick", at=@At(value="INVOKE",
        target="Lnet/minecraft/world/item/component/Consumable;shouldEmitParticlesAndSounds(I)Z"), require=1)
    private boolean holyLoisEarlyBites(Consumable food, int remaining, Operation<Boolean> original,
                                     Level level, LivingEntity user, int ticks) {
        var stack=(ItemStack)(Object)this;
        int total=stack.getUseDuration(user);
        if(FishData.weighed(stack)&&total>32) return Math.max(0,total-remaining)%4==0;
        return original.call(food,remaining);
    }
}
