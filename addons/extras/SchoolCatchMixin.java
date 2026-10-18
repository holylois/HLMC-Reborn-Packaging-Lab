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

/** A hooked swimming fish, or one swimming next to the bobber at a bite, arrives as the fish item (SchoolCatch). */
@Mixin(FishingHook.class)
public abstract class SchoolCatchMixin {
    @Shadow private Entity hookedIn;
    @Shadow private int nibble;

    @Shadow public abstract net.minecraft.world.entity.player.Player getPlayerOwner();

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void holyLoisReelFish(ItemStack rod, CallbackInfoReturnable<Integer> info) {
        if (!(getPlayerOwner() instanceof ServerPlayer angler)) return;
        var fish = hookedIn instanceof AbstractFish hooked ? hooked : hookedIn == null && nibble > 0 ? SchoolCatch.seen((FishingHook)(Object)this) : null;
        if (fish == null) return;
        if (!SchoolCatch.reel(angler, fish,((FishingHook)(Object)this).position())) return;
        var hook=(FishingHook)(Object)this;
        net.minecraft.advancements.triggers.CriteriaTriggers.FISHING_ROD_HOOKED.trigger(angler,rod,hook,java.util.List.of(SchoolCatch.itemFor(fish)));
        angler.level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(angler.level(),angler.getX(),angler.getY()+0.5,angler.getZ(),angler.getRandom().nextInt(6)+1));
        ((FishingHook) (Object) this).discard();
        info.setReturnValue(1);
    }
}
