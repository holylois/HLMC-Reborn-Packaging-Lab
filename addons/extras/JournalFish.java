package holylois.boombox;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Only the caught appearance and weight, never arbitrary item or entity data. */
public record JournalFish(String species,String variant,double kg,String rarity,boolean shiny,long time,String dimension,int x,int y,int z) {
    public JournalFish(String species,String variant,double kg,String rarity,boolean shiny,long time){this(species,variant,kg,rarity,shiny,time,"",0,0,0);}
    public JournalFish {
        if(dimension==null)dimension="";
        if(!dimension.isEmpty()&&(dimension.length()>128||!dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")))throw new IllegalArgumentException("Invalid catch dimension");
        if(species==null||!species.matches("[a-z0-9_.-]+:[a-z0-9_./-]{1,100}")||variant==null||variant.length()>128||!variant.isEmpty()&&!variant.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||!Double.isFinite(kg)||kg<0||kg>FishData.MAX_KG||!java.util.List.of("common","uncommon","rare","epic","legendary","mythic").contains(rarity)||time<0)throw new IllegalArgumentException("Invalid caught fish appearance");
    }
    public static JournalFish capture(ItemStack stack,long time){
        String species=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),variant="";
        if(species.startsWith("fishofthieves:")){
            var component=BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(Identifier.parse(species+"/variant"));
            if(component.isPresent()&&stack.get(component.get()) instanceof net.minecraft.core.Holder<?> holder)variant=holder.unwrapKey().map(k->k.identifier().toString()).orElse("");
        }
        String rarity=FishData.rarity(stack);if(rarity.isEmpty())rarity="common";
        return new JournalFish(species,variant,FishData.kilograms(stack),rarity,FishData.shiny(stack),time);
    }
    public boolean betterThan(JournalFish old){return old==null||kg>old.kg||kg==old.kg&&(rank(rarity)>rank(old.rarity)||rarity.equals(old.rarity)&&shiny&&!old.shiny);}
    public boolean rarerThan(JournalFish old){return old==null||rank(rarity)>rank(old.rarity)||rank(rarity)==rank(old.rarity)&&(shiny&&!old.shiny||shiny==old.shiny&&kg>old.kg);}
    public static int rank(String rarity){return java.util.List.of("common","uncommon","rare","epic","legendary","mythic").indexOf(rarity);}
    public boolean location(){return !dimension.isEmpty();}
    public JournalFish at(String dimension,int x,int y,int z){return new JournalFish(species,variant,kg,rarity,shiny,time,dimension,x,y,z);}
    public DefeatRecord presentation(){return new DefeatRecord(species,0,1,false,false,time,"minecraft:overworld",0,0,0,java.util.List.of(),java.util.List.of(),java.util.List.of(),"",kg>0?SwimmingFish.scale(kg):1);}
}
