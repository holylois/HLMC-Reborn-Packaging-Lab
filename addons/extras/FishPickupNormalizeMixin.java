package holylois.boombox.mixins;

import holylois.boombox.LowTierFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Old stacks must be canonical before vanilla chooses a destination slot, not on the next periodic sweep. */
@Mixin(ItemEntity.class)
public abstract class FishPickupNormalizeMixin {
    @Inject(method="playerTouch",at=@At("HEAD"),require=1)
    private void holyLoisBeforePickup(Player player,CallbackInfo info){
        if(player instanceof ServerPlayer serverPlayer)LowTierFish.beforePickup(serverPlayer,(ItemEntity)(Object)this);
    }
}
