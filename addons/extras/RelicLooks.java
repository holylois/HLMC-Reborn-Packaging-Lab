package holylois.boombox;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.Set;

/** Relics with their own art (assets/holylois/items/relic): a flat icon in menus, frames and on the ground, a 3D model in hand. */
public final class RelicLooks {
    private RelicLooks() {}
    /** Off for 2.2.0 (owner 2026-10-19: relics keep their vanilla looks until the proper relic art lands); turn back on with
     *  "staff_of_quiet_roads", "loiss_lantern", "drowned_bell_clapper", "bell_keepers_hook", "first_rune_hammer",
     *  "unfinished_blade", "circlet_of_the_wanderer", "smiths_apron_scrap". */
    static final Set<String> DRAWN = Set.of();
    /** Worn relics with their own armor texture (assets/holylois/equipment). Off for 2.2.0 (owner 2026-10-19: faces still
     *  flicker); turn back on with "circlet_of_the_wanderer", key("circlet"), "smiths_apron_scrap", key("smiths_apron"). */
    static final java.util.Map<String, net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset>> WORN = java.util.Map.of();

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> key(String name) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("holylois", name));
    }

    public static net.minecraft.resources.ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> worn(ItemStack stack) {
        if (stack.isEmpty() || !stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) return null;
        return WORN.get(RelicPowers.id(stack));
    }
    /** Base items of drawn relics: a cheap check before reading custom data, which runs for every rendered item. */
    static final Set<net.minecraft.world.item.Item> BASES = Set.of(net.minecraft.world.item.Items.STICK, net.minecraft.world.item.Items.LANTERN,
        net.minecraft.world.item.Items.IRON_SWORD, net.minecraft.world.item.Items.FISHING_ROD, net.minecraft.world.item.Items.IRON_AXE,
        net.minecraft.world.item.Items.MACE, net.minecraft.world.item.Items.STONE_SWORD,
        net.minecraft.world.item.Items.GOLDEN_HELMET, net.minecraft.world.item.Items.LEATHER_CHESTPLATE);

    public static Identifier model(ItemStack stack) {
        if (stack.isEmpty() || !BASES.contains(stack.getItem()) || !stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) return null;
        String id = RelicPowers.id(stack);
        return DRAWN.contains(id) ? Identifier.fromNamespaceAndPath("holylois", "relic/" + id) : null;
    }
}
