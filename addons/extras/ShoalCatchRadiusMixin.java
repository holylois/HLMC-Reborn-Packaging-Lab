package holylois.boombox.mixins;

import com.stevekung.fishofthieves.entity.shoal.Shoal;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import holylois.boombox.ShoalFishIdentity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.List;
import java.util.function.Predicate;

/** A cast inside a widened visible circle must reach that shoal, without catching unrelated smaller circles nearby. */
@Mixin(Shoal.class)
public abstract class ShoalCatchRadiusMixin {
    @ModifyArg(method="fishUpShoal",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/AABB;inflate(D)Lnet/minecraft/world/phys/AABB;"),index=0,require=2)
    private static double holyLoisSearch(double old){return Math.max(old,3.5);}
    @WrapOperation(method="fishUpShoal",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"),require=1)
    private static List<Shoal> holyLoisActualCircle(Level level,Class<Shoal> type,AABB box,Predicate<Shoal> predicate,
                                                  Operation<List<Shoal>> original,Entity hook,Player angler){
        return original.call(level,type,box,predicate).stream().filter(shoal->{
            double radius=ShoalFishIdentity.radius(shoal)+0.5,dx=hook.getX()-shoal.getX(),dz=hook.getZ()-shoal.getZ();
            return dx*dx+dz*dz<=radius*radius&&Math.abs(hook.getY()-shoal.getY())<=1.5;
        }).sorted(java.util.Comparator.comparingDouble(shoal->shoal.distanceToSqr(hook))).toList();
    }
}
