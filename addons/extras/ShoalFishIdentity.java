package holylois.boombox;

import com.stevekung.fishofthieves.entity.shoal.Shoal;
import com.stevekung.fishofthieves.entity.shoal.ShoalFishData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;

/** Prepare bounded virtual fish without adding mobs/AI to the world. Preserve existing FoT variants and UUIDs. */
public final class ShoalFishIdentity {
    private ShoalFishIdentity(){}
    public static void prepare(Shoal shoal){
        if(!(shoal.level() instanceof ServerLevel level))return;
        var list=shoal.getShoalFish();
        for(int i=0;i<Math.min(ShoalFishData.MAX_SHOAL_FISH,list.size());i++){
            var entry=list.get(i);
            if(entry.data().contains(SwimmingFish.SAVE_KEY))continue;
            var tag=entry.data().copy();tag.putString("id",entry.id());
            var entity=EntityType.loadEntityRecursive(tag,level,new EntitySpawnRequest(EntitySpawnReason.LOAD,false),EntityProcessor.NOP);
            if(entity instanceof AbstractFish fish){
                SwimmingFish.identity(fish);
                var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());fish.saveWithoutId(output);
                list.set(i,new ShoalFishData(entry.id(),entry.uuid(),output.buildResult()));
            }
        }
    }
    public static void addVanilla(Shoal shoal){
        if(!(shoal.level() instanceof ServerLevel level)||shoal.getShoalFish().size()>=ShoalFishData.MAX_SHOAL_FISH||shoal.getShoalFish().stream().anyMatch(f->f.id().startsWith("minecraft:")))return;
        var biome=level.getBiome(shoal.blockPosition());
        boolean warm=biome.is(net.minecraft.world.level.biome.Biomes.WARM_OCEAN)||biome.is(net.minecraft.world.level.biome.Biomes.LUKEWARM_OCEAN)||biome.is(net.minecraft.world.level.biome.Biomes.DEEP_LUKEWARM_OCEAN);
        String species=warm?(level.getRandom().nextInt(4)==0?"pufferfish":"tropical_fish"):
            biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN)?"cod":"salmon";
        EntityType<?> type=BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(species));
        var fish=(AbstractFish)type.create(level,EntitySpawnReason.NATURAL);if(fish==null)return;
        fish.setPos(shoal.position());fish.finalizeSpawn(level,level.getCurrentDifficultyAt(shoal.blockPosition()),EntitySpawnReason.NATURAL,null);
        SwimmingFish.identity(fish);
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());fish.saveWithoutId(output);
        shoal.getShoalFish().add(new ShoalFishData(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),fish.getUUID(),output.buildResult()));
    }
    public static float radius(float largest,int count){return (float)Math.clamp(0.6+largest*0.45+Math.sqrt(Math.max(1,count))*0.12,1,3.0);}
    public static float radius(Shoal shoal){
        double largest=1;
        for(var fish:shoal.getShoalFish())largest=Math.max(largest,fish.data().getDoubleOr(SwimmingFish.SCALE_KEY,1));
        return radius((float)largest,shoal.getShoalFish().size());
    }
}
