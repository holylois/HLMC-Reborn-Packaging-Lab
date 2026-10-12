package holylois.boombox;

import java.util.List;

/** A bounded visual/statistical snapshot, never full entity or item NBT. */
public record DefeatRecord(String type,int level,float health,boolean elite,boolean baby,long time,String dimension,
                           int x,int y,int z,List<Gear> gear,List<Effect> effects,List<String> affixes,String appearanceId,float scale) {
    public DefeatRecord(String type,int level,float health,boolean elite,boolean baby,long time,String dimension,int x,int y,int z,List<Gear> gear,List<Effect> effects,List<String> affixes){this(type,level,health,elite,baby,time,dimension,x,y,z,gear,effects,affixes,"",1);}
    public record Gear(String slot,String item,int dye,boolean glint,String material,String pattern,String model,String modelData){
        public Gear{if(!List.of("head","chest","legs","feet","mainhand","offhand").contains(slot))throw new IllegalArgumentException("Invalid gear slot");id(item);optional(material);optional(pattern);optional(model);if(modelData==null||modelData.length()>1024)throw new IllegalArgumentException("Oversized model data");if(!modelData.isEmpty()&&net.minecraft.world.item.component.CustomModelData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(modelData)).result().isEmpty())throw new IllegalArgumentException("Invalid model data");}
    }
    public record Effect(String id,int amplifier,int ticks){public Effect{DefeatRecord.id(id);if(amplifier<0||amplifier>255||ticks< -1||ticks>1_000_000)throw new IllegalArgumentException("Invalid effect");}}
    public DefeatRecord {
        id(type);id(dimension);gear=List.copyOf(gear);effects=List.copyOf(effects);affixes=List.copyOf(affixes);
        if(appearanceId==null||!Float.isFinite(scale)||scale<=0||scale>16)throw new IllegalArgumentException("Invalid appearance identity");if(!appearanceId.isEmpty())java.util.UUID.fromString(appearanceId);
        if(level<0||level>1_000_000||!Float.isFinite(health)||health<=0||health>1_000_000||time<0||gear.size()>6||effects.size()>16||affixes.size()>16)throw new IllegalArgumentException("Invalid defeat snapshot");
        if(gear.stream().map(Gear::slot).distinct().count()!=gear.size())throw new IllegalArgumentException("Duplicate gear slot");
        for(var affix:affixes)if(affix==null||!affix.matches("[a-z0-9_.:/-]{1,128}"))throw new IllegalArgumentException("Invalid affix id");
    }
    private static void id(String value){if(value==null||value.length()>128||!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("Invalid registry id");}
    private static void optional(String value){if(value==null)throw new IllegalArgumentException("Missing appearance field");if(!value.isEmpty())id(value);}
    /** Level first, then maximum health, then elite. Exact ties retain the earlier confirmed record. */
    public boolean strongerThan(DefeatRecord old){return old==null||level>old.level||level==old.level&&(health>old.health||health==old.health&&elite&&!old.elite);}
    public interface Memory{DefeatRecord holyLoisDefeat();void holyLoisDefeat(DefeatRecord record);}
}
