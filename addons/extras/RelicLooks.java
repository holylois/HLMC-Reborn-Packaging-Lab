package holylois.boombox;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.Set;

/** Relics with their own art (assets/holylois/items/relic): a flat icon in menus, frames and on the ground, a 3D model in hand. */
public final class RelicLooks {
    private RelicLooks() {}
    static final Set<String> DRAWN = Set.of("staff_of_quiet_roads", "loiss_lantern", "drowned_bell_clapper", "bell_keepers_hook",
        "first_rune_hammer", "unfinished_blade");
    /** Base items of drawn relics: a cheap check before reading custom data, which runs for every rendered item. */
    static final Set<net.minecraft.world.item.Item> BASES = Set.of(net.minecraft.world.item.Items.STICK, net.minecraft.world.item.Items.LANTERN,
        net.minecraft.world.item.Items.IRON_SWORD, net.minecraft.world.item.Items.FISHING_ROD, net.minecraft.world.item.Items.IRON_AXE,
        net.minecraft.world.item.Items.MACE, net.minecraft.world.item.Items.STONE_SWORD);

    public static Identifier model(ItemStack stack) {
        if (stack.isEmpty() || !BASES.contains(stack.getItem()) || !stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) return null;
        String id = RelicPowers.id(stack);
        return DRAWN.contains(id) ? Identifier.fromNamespaceAndPath("holylois", "relic/" + id) : null;
    }
}
