package holylois.boombox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.LinkedHashMap;

/** Cached isolated living-mob previews, no world insertion or ticking, no arbitrary entity data. */
public final class JournalPreview {
    private final LinkedHashMap<DefeatRecord,LivingEntity> cache=new LinkedHashMap<>();
    private Object level;
    private long startTick=-1;
    private static int nextPreviewId=-1_000_000;
    public LivingEntity entity(DefeatRecord record){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||record==null||record.type().equals("minecraft:player"))return null;
        if(level!=mc.level){cache.clear();level=mc.level;}
        if(cache.containsKey(record))return cache.get(record);
        if(cache.size()>=8)cache.remove(cache.keySet().iterator().next());
        var type=BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(record.type()));if(type.isEmpty())return null;
        var created=type.get().create(mc.level,EntitySpawnReason.LOAD);if(!(created instanceof LivingEntity zombie))return null;
        zombie.setId(nextPreviewId--);if(!record.appearanceId().isEmpty())zombie.setUUID(java.util.UUID.fromString(record.appearanceId()));
        if(zombie instanceof Mob mob)mob.setNoAi(true);
        zombie.setNoGravity(true);zombie.setOnGround(true);zombie.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        zombie.setSilent(true);zombie.setPermanentlyInvulnerable(true);
        if(zombie instanceof Zombie z)z.setBaby(record.baby());else if(zombie instanceof AgeableMob a)a.setBaby(record.baby());
        var scale=zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);if(scale!=null)scale.setBaseValue(record.scale());
        zombie.refreshDimensions();
        zombie.setPos(mc.player.position());
        for(var gear:record.gear()){
            var item=BuiltInRegistries.ITEM.getOptional(Identifier.parse(gear.item()));if(item.isEmpty())continue;var stack=new ItemStack(item.get());
            if(gear.dye()>=0)stack.set(DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(gear.dye()));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,gear.glint());
            if(!gear.model().isEmpty())stack.set(DataComponents.ITEM_MODEL,Identifier.parse(gear.model()));
            if(!gear.modelData().isEmpty())net.minecraft.world.item.component.CustomModelData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(gear.modelData())).result().ifPresent(v->stack.set(DataComponents.CUSTOM_MODEL_DATA,v));
            if(!gear.material().isEmpty()&&!gear.pattern().isEmpty()){
                var materials=mc.level.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);var patterns=mc.level.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
                var material=materials.get(net.minecraft.resources.ResourceKey.create(Registries.TRIM_MATERIAL,Identifier.parse(gear.material())));var pattern=patterns.get(net.minecraft.resources.ResourceKey.create(Registries.TRIM_PATTERN,Identifier.parse(gear.pattern())));
                if(material.isPresent()&&pattern.isPresent())stack.set(DataComponents.TRIM,new net.minecraft.world.item.equipment.trim.ArmorTrim(material.get(),pattern.get()));
            }
            var slot=java.util.Arrays.stream(net.minecraft.world.entity.EquipmentSlot.values()).filter(s->s.getName().equals(gear.slot())).findFirst();slot.ifPresent(s->zombie.setItemSlot(s,stack));
        }
        cache.put(record,zombie);return zombie;
    }
    public boolean draw(GuiGraphicsExtractor g,DefeatRecord record,int x,int y,int width,int height,boolean portrait,float rotation){
        return draw(g,record,null,x,y,width,height,portrait,rotation);
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public boolean draw(GuiGraphicsExtractor g,DefeatRecord record,JournalFish caught,int x,int y,int width,int height,boolean portrait,float rotation){
        var entity=entity(record);if(entity==null)return false;
        if(entity instanceof com.stevekung.fishofthieves.entity.ThievesFish fish){
            fish.setNoFlip(true);
            if(caught!=null&&!caught.variant().isEmpty()){
                var registry=Minecraft.getInstance().level.registryAccess().lookupOrThrow(fish.getRegistryKey());
                registry.get(net.minecraft.resources.ResourceKey.create(fish.getRegistryKey(),Identifier.parse(caught.variant()))).ifPresent(value->fish.setVariant(value));
            }
        }
        var renderer=Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        // Fresh Animations (EMF) adds the game's own partial tick to the entity's tick count, so the preview clock must follow
        // game ticks too: a separate wall clock drifted against it inside each tick and made bodies (spiders) jump.
        var mc=Minecraft.getInstance();long now=mc.level.getGameTime();if(startTick<0||now<startTick)startTick=now;
        boolean motion=JournalHints.motionEnabled();
        float partial=motion?mc.getDeltaTracker().getGameTimeDeltaPartialTick(true):0;
        entity.tickCount=motion?(int)(now-startTick):0;
        float age=entity.tickCount+partial;
        if(entity instanceof net.minecraft.world.entity.animal.fish.AbstractFish)((holylois.boombox.mixins.EntityWaterAccessor)entity).holyLois$setInWater(true);
        // A witch recorded mid-drink still had the potion in hand; the preview shows it idle.
        if(entity instanceof net.minecraft.world.entity.monster.Witch witch)witch.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,ItemStack.EMPTY);
        var state=renderer.createRenderState(entity,partial);
        idleState(state,age,entity instanceof net.minecraft.world.entity.animal.fish.AbstractFish);
        state.shadowPieces.clear();state.outlineColor=0;
        state.lightCoords=net.minecraft.util.LightCoordsUtil.pack(15,15);
        float span=record.type().equals("minecraft:cave_spider")?4.5f:record.type().equals("minecraft:spider")?2.7f:2.0f;
        boolean fish=entity instanceof net.minecraft.world.entity.animal.fish.AbstractFish;
        // FoT uses fixed collision dimensions even when the rendered trophy is scaled.
        float visualWidth=fish?2.5f*entity.getScale():entity.getBbWidth()*span;
        float visualHeight=fish?0.8f*entity.getScale():entity.getBbHeight()*1.3f;
        int scale=portrait?40:Math.max(1,(int)Math.min((width-12)/Math.max(0.1f,visualWidth),(height-12)/Math.max(0.1f,visualHeight)));
        float focus=portrait?(entity.isBaby()?0.82f:1.65f):fish?entity.getScale()*0.35f:entity.getBbHeight()*0.55f;
        g.entity(state,scale,new Vector3f(0,focus,0),new Quaternionf().rotateZ((float)Math.PI).rotateY(rotation),new Quaternionf(),x,y,x+width,y+height);
        if(!portrait&&record.elite()&&!record.affixes().isEmpty())aura(g,record,x,y,width,height,age);
        return true;
    }
    public static void idleState(net.minecraft.client.renderer.entity.state.EntityRenderState state,float age,boolean fish){
        state.ageInTicks=age;
        if(state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living){living.bodyRot=180;living.yRot=0;living.xRot=0;living.deathTime=0;living.walkAnimationSpeed=0;living.walkAnimationPos=0;living.isInWater=fish;living.pose=net.minecraft.world.entity.Pose.STANDING;}
        if(state instanceof net.minecraft.client.renderer.entity.state.SlimeRenderState slime)slime.squish=0;
        if(state instanceof net.minecraft.client.renderer.entity.state.ChickenRenderState chicken){chicken.flap=0;chicken.flapSpeed=0;}
        if(state instanceof net.minecraft.client.renderer.entity.state.WitchRenderState witch){witch.isHoldingPotion=false;witch.isHoldingItem=false;}
    }
    /** Bounded GUI-only motes in the recorded elite palette; never spawn world particles. */
    private static void aura(GuiGraphicsExtractor g,DefeatRecord record,int x,int y,int width,int height,float age){
        int color=auraColor(record.affixes().size());float clock=JournalHints.motionEnabled()?age:0;
        g.nextStratum();g.enableScissor(x,y,x+width,y+height);
        for(int i=0;i<10;i++){
            double angle=i*2.39996+clock*0.035;double rise=((i*0.113+clock*0.009)%1);
            int px=x+width/2+(int)(Math.cos(angle)*Math.min(width*0.22,height*0.35));int py=y+height-4-(int)(rise*(height-8));
            g.fill(px,py,px+1,py+1,0xCC000000|color);if(i%3==0)g.fill(px-1,py,px+2,py+1,0x66000000|color);
        }
        g.disableScissor();
    }
    public static int auraColor(int count){
        try{var quality=Class.forName("com.ultimatearpg.item.ArpgItemQuality");var value=quality.getMethod("fromEliteAffixCount",int.class).invoke(null,count);return (int)quality.getMethod("color").invoke(value)&0xFFFFFF;}catch(ReflectiveOperationException|LinkageError ignored){return Ui.GOLD&0xFFFFFF;}
    }
    public static DefeatRecord presentation(JournalState.Entry entry){
        if(entry.kind()==JournalState.FISH)return entry.bestFish()!=null?entry.bestFish().presentation():new JournalFish(entry.name(),"",entry.weight(),"common",false,0).presentation();
        if(entry.strongest()!=null)return entry.strongest();
        return entry.subject().isEmpty()?null:new DefeatRecord(entry.subject(),0,20,false,false,0,"minecraft:overworld",0,0,0,java.util.List.of(),java.util.List.of(),java.util.List.of());
    }
}
