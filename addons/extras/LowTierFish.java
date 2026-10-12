package holylois.boombox;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import java.util.ArrayList;

/** Catch-specific data belongs to trophies, not stackable meals. Swimming mass stays on the fish entity. */
public final class LowTierFish {
    private LowTierFish() {}
    public static void stripBucketAppearance(ItemStack stack){
        if(!stack.has(DataComponents.FOOD))return;
        stack.remove(DataComponents.TROPICAL_FISH_PATTERN);
        stack.remove(DataComponents.TROPICAL_FISH_BASE_COLOR);
        stack.remove(DataComponents.TROPICAL_FISH_PATTERN_COLOR);
    }
    public static boolean normalize(ItemStack stack){
        if(stack.isEmpty()||!stack.has(DataComponents.FOOD))return false;
        boolean changed=stack.has(DataComponents.TROPICAL_FISH_PATTERN)||stack.has(DataComponents.TROPICAL_FISH_BASE_COLOR)||stack.has(DataComponents.TROPICAL_FISH_PATTERN_COLOR);
        stripBucketAppearance(stack);
        var fish=FishData.tag(stack);if(fish==null)return changed;
        String rarity=fish.getStringOr("rarity","");
        if(!rarity.equals("common")&&!rarity.equals("uncommon"))return changed;
        if(fish.getIntOr("meal_version",0)==1)return changed;
        var tag=stack.get(DataComponents.CUSTOM_DATA).copyTag();tag.remove(Legends.FISH_KEY);
        for(var type:java.util.List.of(DataComponents.ITEM_NAME,DataComponents.LORE,DataComponents.FOOD,DataComponents.CONSUMABLE,
            DataComponents.MAX_STACK_SIZE,DataComponents.ATTRIBUTE_MODIFIERS,DataComponents.ENCHANTMENT_GLINT_OVERRIDE))restore(stack,type);
        if(rarity.equals("uncommon")){
            var meal=new CompoundTag();meal.putString("rarity",rarity);meal.putInt("meal_version",1);
            tag.put(Legends.FISH_KEY,meal);
            int rgb=FishData.color(rarity)&0xFFFFFF;
            stack.set(DataComponents.ITEM_NAME,Component.literal(stack.getHoverName().getString()).withStyle(s->s.withColor(rgb).withItalic(false)));
            var lines=new ArrayList<Component>();
            lines.add(Component.literal("✦ Uncommon catch").withStyle(s->s.withColor(rgb).withItalic(false)));
            String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            lines.addAll(FishTraits.apply(stack,id.replace("cooked_",""),rarity,0,false,id.contains("cooked_"),1,false));
            stack.set(DataComponents.LORE,new ItemLore(lines));
        }
        if(tag.isEmpty())stack.remove(DataComponents.CUSTOM_DATA);else stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        return true;
    }
    private static <T> void restore(ItemStack stack,DataComponentType<T> type){
        T value=stack.getItem().components().get(type);if(value==null)stack.remove(type);else stack.set(type,value);
    }
    static void register(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{
            if(entity instanceof net.minecraft.world.entity.item.ItemEntity item)normalize(item.getItem());
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server->{
            if(server.getTickCount()%20!=0)return;
            for(var player:server.getPlayerList().getPlayers()){
                boolean changed=false;
                for(var slot:player.inventoryMenu.slots)changed|=normalize(slot.getItem());
                if(changed)player.inventoryMenu.broadcastChanges();
                if(player.containerMenu!=player.inventoryMenu){
                    changed=false;for(var slot:player.containerMenu.slots)changed|=normalize(slot.getItem());
                    if(changed)player.containerMenu.broadcastChanges();
                }
            }
        });
    }
}
