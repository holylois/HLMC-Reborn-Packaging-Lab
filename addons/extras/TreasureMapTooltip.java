package holylois.boombox;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The map number identifies a real saved map; present it as an inscription without inventing map artwork. */
public final class TreasureMapTooltip {
    private TreasureMapTooltip() {}
    public static boolean matches(ItemStack stack){return stack.is(Items.BURIED_TREASURE_MAP)&&!stack.has(DataComponents.CUSTOM_NAME);}
    public static Component name(){return Component.translatable("holylois.map.treasure");}
    static void register(){net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,flags,lines)->{
        if(!matches(stack)||lines.isEmpty())return;
        lines.set(0,name());
        if(!flags.isAdvanced())lines.removeIf(line->FishTooltip.hasKey(line,"filled_map.id"));
        lines.add(Component.translatable("holylois.map.clue").withStyle(ChatFormatting.GRAY));
        var id=stack.get(DataComponents.MAP_ID);
        if(id!=null)lines.add(Component.translatable("holylois.map.inscription",id.id()).withStyle(s->s.withColor(ChatFormatting.DARK_GRAY).withItalic(true)));
    });}
}
