package holylois.boombox;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Crate presentation shares the fish palette; this does not change loot contents or drop chances. */
public final class CrateStyle {
    public static String rarity(String id){return switch(id){
        case "fishing_crates:wooden_crate"->"uncommon";
        case "fishing_crates:iron_crate"->"rare";
        case "fishing_crates:golden_crate"->"legendary";
        default->"";
    };}
    public static String rarity(ItemStack stack){return rarity(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());}
    public static Component name(ItemStack stack){
        String tier=rarity(stack);var name=stack.getHoverName();if(tier.isEmpty())return name;
        return Component.literal(net.minecraft.ChatFormatting.stripFormatting(name.getString())).withStyle(s->FishData.shine(s.withColor(FishData.color(tier)&0xFFFFFF),tier));
    }
    static void register(){net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,flags,lines)->{
        String tier=rarity(stack);
        if(!lines.isEmpty()&&!tier.isEmpty()){
            lines.set(0,name(stack));
            // Replace the mod's vague flavor line with a clear action and reward description.
            if(lines.size()>1)lines.set(1,Component.translatable("holylois.crate.tip").withStyle(net.minecraft.ChatFormatting.GRAY));
            else lines.add(Component.translatable("holylois.crate.tip").withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.add(2,Component.translatable("holylois.crate.open").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    });}
}
