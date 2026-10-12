package holylois.boombox;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.ArrayList;

/** Put our held bonuses inside the fish lore so food-tooltip mods append their icons after all text. */
public final class FishTooltipSections {
    private FishTooltipSections() {}
    static boolean normalize(ItemStack stack){
        var fish=FishData.tag(stack);var attributes=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if(!FishData.weighed(stack)||fish.getBooleanOr("held_tooltip",false)||attributes==null||attributes.modifiers().isEmpty())return false;
        var lore=stack.get(DataComponents.LORE);if(lore==null)return false;
        var lines=new ArrayList<Component>();var credit=new ArrayList<Component>();
        for(var line:lore.lines()){
            if(line.getString().startsWith("Caught by ")||line.getString().startsWith("Caught on "))credit.add(line);else lines.add(line);
        }
        lines.add(Component.translatable("holylois.fish.held").withStyle(s->s.withColor(ChatFormatting.GRAY).withItalic(false)));
        for(var entry:attributes.modifiers()){
            var modifier=entry.modifier();boolean percent=modifier.operation()!=AttributeModifier.Operation.ADD_VALUE;
            double amount=modifier.amount()*(percent?100:1);
            String value=java.math.BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString();
            lines.add(Component.literal((amount>=0?"+":"")+value+(percent?"% ":" "))
                .append(Component.translatable(entry.attribute().value().getDescriptionId())).withStyle(s->s.withColor(ChatFormatting.BLUE).withItalic(false)));
        }
        lines.addAll(credit);stack.set(DataComponents.LORE,new ItemLore(lines));
        stack.set(DataComponents.TOOLTIP_DISPLAY,stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY,TooltipDisplay.DEFAULT).withHidden(DataComponents.ATTRIBUTE_MODIFIERS,true));
        var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();fish.putBoolean("held_tooltip",true);data.put(Legends.FISH_KEY,fish);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return true;
    }
}
