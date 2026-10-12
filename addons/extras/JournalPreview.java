package holylois.boombox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

/** Zombie pilot: cached isolated preview objects, no world insertion or ticking, no arbitrary entity data. */
public final class JournalPreview {
    private final LinkedHashMap<DefeatRecord,Zombie> cache=new LinkedHashMap<>();
    private Object level;
    private final long clockStarted=net.minecraft.util.Util.getMillis();
    private static int nextPreviewId=-1_000_000;
    public Zombie entity(DefeatRecord record){
        var mc=Minecraft.getInstance();if(mc.level==null||record==null||!record.type().equals("minecraft:zombie"))return null;
        if(level!=mc.level){cache.clear();level=mc.level;}
        if(cache.containsKey(record))return cache.get(record);
        if(cache.size()>=8)cache.remove(cache.keySet().iterator().next());
        var zombie=(Zombie)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:zombie")).create(mc.level,EntitySpawnReason.LOAD);if(zombie==null)return null;
        zombie.setId(nextPreviewId--);if(!record.appearanceId().isEmpty())zombie.setUUID(java.util.UUID.fromString(record.appearanceId()));
        zombie.setNoAi(true);zombie.setSilent(true);zombie.setPermanentlyInvulnerable(true);zombie.setBaby(record.baby());
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
        var entity=entity(record);if(entity==null)return false;
        var renderer=Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        float age=(net.minecraft.util.Util.getMillis()-clockStarted)/50f;
        entity.tickCount=(int)age;
        var state=renderer.createRenderState(entity,age-(int)age);
        state.shadowPieces.clear();state.outlineColor=0;
        state.lightCoords=net.minecraft.util.LightCoordsUtil.pack(15,15);
        if(state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living){living.bodyRot=180;living.yRot=0;living.xRot=0;living.deathTime=0;living.walkAnimationSpeed=0;living.walkAnimationPos=0;}
        int scale=portrait?40:Math.max(12,Math.min(width/2,height/2));
        float focus=portrait?(entity.isBaby()?0.82f:1.65f):entity.getBbHeight()/2;
        g.entity(state,scale,new Vector3f(0,focus,0),new Quaternionf().rotateZ((float)Math.PI).rotateY(rotation),new Quaternionf(),x,y,x+width,y+height);
        return true;
    }
    public static DefeatRecord presentation(JournalState.Entry entry){
        if(entry.strongest()!=null)return entry.strongest();
        return entry.subject().equals("minecraft:zombie")?new DefeatRecord("minecraft:zombie",0,20,false,false,0,"minecraft:overworld",0,0,0,java.util.List.of(),java.util.List.of(),java.util.List.of()):null;
    }
}
