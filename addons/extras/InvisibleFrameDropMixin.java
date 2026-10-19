package holylois.boombox.mixins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Invisible item frames are structure decoration (dungeon loot shelves): punching one still drops the item inside, but the frame
 * itself never drops as an item frame (owner 2026-10-10). Survival players cannot make invisible frames, so nothing is lost.
 * Gear on such a shelf sometimes comes out as a Runeforged piece (RuneforgedFrameLoot), once per frame.
 */
@Mixin(ItemFrame.class)
public abstract class InvisibleFrameDropMixin {
    @Inject(method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V", at = @At("HEAD"))
    private void holyLoisShelfLoot(ServerLevel level, Entity breaker, boolean dropFrame, CallbackInfo info) {
        var frame = (ItemFrame) (Object) this;
        // One roll per shelf frame, ever: the empty frame stays, so without the mark a player could refill it with their own gear
        // and punch it out again and again for free Runeforged rolls (audit 2026-10-19).
        if (frame.isInvisible() && !frame.getItem().isEmpty() && frame.addTag(holylois.boombox.RuneforgedFrameLoot.ROLLED))
            holylois.boombox.RuneforgedFrameLoot.roll(frame.getItem(), level.getRandom());
    }

    @ModifyVariable(method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean holyLoisKeepInvisibleFrame(boolean dropFrame, ServerLevel level, Entity breaker) {
        return dropFrame && !((Entity) (Object) this).isInvisible();
    }
}
