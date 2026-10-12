package holylois.boombox.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/** Shoal catches bypass FishingHook.hookedIn and spawn a live fish. Convert that specific spawn, retaining the mod's XP/shoal flow. */
@Mixin(targets="com.stevekung.fishofthieves.entity.shoal.Shoal")
public abstract class ShoalItemCatchMixin {
    @WrapOperation(method="fishUpShoal",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),require=1)
    private static boolean holyLoisShoalItem(Level level,Entity entity,Operation<Boolean> original,Entity hook,Player player){
        if(entity instanceof AbstractFish fish&&player instanceof ServerPlayer angler&&holylois.boombox.SchoolCatch.reel(angler,fish))return true;
        return original.call(level,entity);
    }
}
