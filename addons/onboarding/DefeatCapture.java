package holylois;

import holylois.boombox.DefeatRecord;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.ArrayList;
import java.util.List;

public final class DefeatCapture {
    private DefeatCapture() {}
    public static DefeatRecord capture(LivingEntity entity){
        int[] rune=Discoveries.Runeforged.levelAndElite(entity);var gear=new ArrayList<DefeatRecord.Gear>();
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND)){
            var stack=entity.getItemBySlot(slot);if(stack.isEmpty())continue;
            var dye=stack.get(DataComponents.DYED_COLOR);var trim=stack.get(DataComponents.TRIM);var model=stack.get(DataComponents.ITEM_MODEL);var custom=stack.get(DataComponents.CUSTOM_MODEL_DATA);
            String json=custom==null?"":net.minecraft.world.item.component.CustomModelData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,custom).result().map(Object::toString).orElse("");
            if(json.length()>1024)json="";
            gear.add(new DefeatRecord.Gear(slot.getName(),BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),dye==null?-1:dye.rgb(),stack.hasFoil(),
                trim==null?"":trim.material().unwrapKey().map(k->k.identifier().toString()).orElse(""),trim==null?"":trim.pattern().unwrapKey().map(k->k.identifier().toString()).orElse(""),model==null?"":model.toString(),json));
        }
        var effects=entity.getActiveEffects().stream().sorted(java.util.Comparator.comparing(e->BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value()).toString())).limit(16)
            .map(e->new DefeatRecord.Effect(BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value()).toString(),Math.clamp(e.getAmplifier(),0,255),Math.clamp(e.getDuration(),-1,1_000_000))).toList();
        float health=Float.isFinite(entity.getMaxHealth())?Math.clamp(entity.getMaxHealth(),1,1_000_000):1;
        float scale=Float.isFinite(entity.getScale())?Math.clamp(entity.getScale(),0.05f,16):1;
        return new DefeatRecord(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),rune==null?0:Math.clamp(rune[0],0,1_000_000),health,rune!=null&&rune[1]>0,entity.isBaby(),System.currentTimeMillis()/1000,
            entity.level().dimension().identifier().toString(),entity.getBlockX(),entity.getBlockY(),entity.getBlockZ(),gear,effects,affixes(entity),entity.getUUID().toString(),scale);
    }
    private static List<String> affixes(LivingEntity entity){
        try{
            var method=Class.forName("com.ultimatearpg.monster.ArpgMonsterLevelAccess").getMethod("ultimatearpg$getEliteAffixIds");
            if(!method.getDeclaringClass().isInstance(entity))return List.of();var raw=method.invoke(entity);
            if(raw instanceof java.util.Collection<?> values)return values.stream().map(Object::toString).filter(s->s.length()<=128&&s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")).sorted().distinct().limit(16).toList();
            if(raw instanceof String text&&text.length()<=4096)return java.util.Arrays.stream(text.split(";")).map(String::strip).filter(s->s.matches("[a-z0-9_.:/-]{1,128}")).sorted().distinct().limit(16).toList();
        }catch(ReflectiveOperationException|LinkageError ignored){}
        return List.of();
    }
}
