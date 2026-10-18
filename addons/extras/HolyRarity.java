package holylois.boombox;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.List;

/**
 * One rarity system, ours (owner 2026-10-18): every item speaks the Holy Lois fish tiers. Weighed fish and fillets keep their
 * own tier, crates theirs; Runeforged qualities map magic -> Uncommon, rare -> Rare, legendary -> Legendary (cursed keeps its
 * own look); vanilla and modded item rarities map yellow -> Uncommon, aqua -> Rare, magenta -> Epic. Enchanting lifts an item
 * one tier (vanilla jumps straight to aqua). The highest source wins. Names, chat links, the outline stroke and the tooltip
 * frame all use {@link FishData#color} and the Legendary/Mythic shine, so a tier looks the same on a fish, a sword or a crate.
 */
public final class HolyRarity {
    public static final List<String> TIERS = List.of("common", "uncommon", "rare", "epic", "legendary", "mythic");
    private static final boolean RUNEFORGED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("runeforged");
    private HolyRarity() {}

    /** The tier of any stack, "" for plain common items. */
    public static String of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        String fish = FishData.rarity(stack);
        if (!fish.isEmpty()) return fish;
        String crate = CrateStyle.rarity(stack);
        if (!crate.isEmpty()) return crate;
        int best = vanilla(stack.getOrDefault(DataComponents.RARITY, Rarity.COMMON), stack.isEnchanted());
        if (RUNEFORGED) best = Math.max(best, Runeforged.rank(stack));
        return best <= 0 ? "" : TIERS.get(best);
    }

    /** Rank of a vanilla rarity component (0 common .. 3 epic), one step up when enchanted, never past Epic. */
    static int vanilla(Rarity rarity, boolean enchanted) {
        int rank = switch (rarity) { case UNCOMMON -> 1; case RARE -> 2; case EPIC -> 3; default -> 0; };
        return enchanted ? Math.min(3, rank + 1) : rank;
    }

    public static int rank(String tier) { return Math.max(0, TIERS.indexOf(tier)); }

    /** The name in its tier colour (and shine for Legendary/Mythic); unchanged for common items. Parts with their own colour keep it. */
    public static Component style(Component name, String tier) {
        int color = FishData.color(tier);
        if (tier.isEmpty() || color == 0 || name == null) return name;
        MutableComponent copy = name.copy();
        return copy.withStyle(s -> FishData.shine(s.withColor(color & 0xFFFFFF), tier));
    }

    /** Separate class so Runeforged is only loaded when installed. */
    private static final class Runeforged {
        static int rank(ItemStack stack) {
            try {
                return switch (com.ultimatearpg.item.ArpgWeaponHelper.peekVisualQuality(stack)) {
                    case MAGIC -> 1; case RARE -> 2; case LEGENDARY -> 4; default -> 0;
                };
            } catch (RuntimeException | LinkageError error) {
                return 0;
            }
        }
    }
}
