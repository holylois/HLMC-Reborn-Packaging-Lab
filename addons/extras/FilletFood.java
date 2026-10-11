package holylois.boombox;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodProperties;

/**
 * Fillets are a snack (owner 2026-10-11): Farmer's Delight cod and salmon slices, raw and cooked, can be eaten with a full hunger
 * bar. Trophy fillets copy this from the item (FishTraits keeps canAlwaysEat). Whole fish stay vanilla: only when you are hungry.
 * Both sides need it, or the client would predict a bite the server refuses.
 */
public final class FilletFood {
    private FilletFood() {}
    static final String[] FILLETS = {"cod_slice", "salmon_slice", "cooked_cod_slice", "cooked_salmon_slice"};

    static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            for (String name : FILLETS) {
                var id = Identifier.fromNamespaceAndPath("farmersdelight", name);
                if (!BuiltInRegistries.ITEM.containsKey(id)) continue;
                var item = BuiltInRegistries.ITEM.getValue(id);
                context.modify(item, builder -> {
                    var food = item.components().get(DataComponents.FOOD);
                    if (food != null) builder.set(DataComponents.FOOD, new FoodProperties(food.nutrition(), food.saturation(), true));
                });
            }
        });
    }
}
