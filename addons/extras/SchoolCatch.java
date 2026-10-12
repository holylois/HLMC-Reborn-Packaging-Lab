package holylois.boombox;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Hooking a swimming fish (Fish of Thieves schools circling a spot, or any vanilla fish) used to pull the live mob to you; from a
 * boat it flopped over the side and swam off (owner 2026-10-11). Reeling it in now launches the matching fish item along the
 * normal fishing arc, retaining its visible Fish of Thieves variant, saved weight and rarity.
 * The fish item has the mob's id (fishofthieves:splashtail, minecraft:cod); mobs without a matching food item stay vanilla.
 */
public final class SchoolCatch {
    private SchoolCatch() {}

    /** The caught item for a hooked fish, or empty when this fish has no item form. Pure enough for tests. */
    public static ItemStack itemFor(AbstractFish fish) {
        var remembered=SwimmingFish.identity(fish);
        return remembered.isEmpty()?rawItemFor(fish):remembered.copy();
    }

    static ItemStack rawItemFor(AbstractFish fish) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(fish.getType());
        var item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isEmpty()) return ItemStack.EMPTY;
        var stack = new ItemStack(item.get());
        if (!stack.has(DataComponents.FOOD)) return ItemStack.EMPTY;
        // The bucket save carries the Fish of Thieves variant; drop what only a bucket needs.
        fish.saveToBucketTag(stack);
        stack.remove(DataComponents.BUCKET_ENTITY_DATA);
        stack.remove(DataComponents.CUSTOM_NAME);
        LowTierFish.stripBucketAppearance(stack);
        return stack;
    }

    /** True when the hooked fish became a flying item (the hook then retrieves nothing else). */
    public static boolean reel(ServerPlayer angler, AbstractFish fish) {
        return reel(angler,fish,fish.position());
    }

    public static boolean reel(ServerPlayer angler,AbstractFish fish,net.minecraft.world.phys.Vec3 origin){
        if (!(fish.level() instanceof ServerLevel level) || !fish.isAlive()) return false;
        var stack = itemFor(fish);
        if (stack.isEmpty()) return false;
        var config = Legends.fish;
        if (config != null && config.enabled) {
            if(FishData.tag(stack)!=null)Legends.stampCatch(stack,level,angler.getName().getString());
            else Legends.weigh(stack, level, angler.getRandom(), angler.getLuck(), angler.getName().getString(), config);
        }
        LowTierFish.normalize(stack);
        FishCaught.record(angler,stack);
        fish.discard();
        angler.awardStat(Stats.FISH_CAUGHT);
        var drop=new ItemEntity(level,origin.x,origin.y,origin.z,stack);
        var delta=angler.position().subtract(origin);
        drop.setDeltaMovement(delta.x*0.1,delta.y*0.1+Math.sqrt(Math.sqrt(delta.lengthSqr()))*0.08,delta.z*0.1);
        level.addFreshEntity(drop);
        level.playSound(null, angler.blockPosition(), net.minecraft.sounds.SoundEvents.FISHING_BOBBER_RETRIEVE, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1f);
        return true;
    }
}
