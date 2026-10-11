package holylois.boombox.mixins;

import holylois.boombox.SchoolCatch;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A hooked swimming fish arrives as the fish item instead of being pulled in alive (SchoolCatch). */
@Mixin(FishingHook.class)
public abstract class SchoolCatchMixin {
    @Shadow private Entity hookedIn;

    @Shadow public abstract net.minecraft.world.entity.player.Player getPlayerOwner();

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void holyLoisReelFish(ItemStack rod, CallbackInfoReturnable<Integer> info) {
        if (!(hookedIn instanceof AbstractFish fish) || !(getPlayerOwner() instanceof ServerPlayer angler)) return;
        if (!SchoolCatch.reel(angler, fish)) return;
        ((FishingHook) (Object) this).discard();
        info.setReturnValue(1);
    }
}
