package holylois.boombox;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.List;

/**
 * One rarity system, ours (owner 2026-10-18): every item speaks the Holy Lois fish tiers. Weighed fish and fillets keep their
 * own tier, crates theirs; Runeforged qualities map magic -> Uncommon, rare -> Rare, legendary -> Legendary, its "Mythic"
 * (legendary plus) -> Mythic, and every cursed kind (Cursed, Alien-Cursed, Rune-Cursed) -> our own Cursed tier, a gamble
 * rather than a step above Legendary; vanilla and modded item rarities map yellow -> Uncommon, aqua -> Rare, magenta -> Epic. Enchanting lifts an item
 * one tier (vanilla jumps straight to aqua). A name given a gold, orange or yellow colour (lootbox tools, legendary loot) is
 * Legendary. The highest source wins. Names, chat links, the outline stroke and the tooltip
 * frame all use {@link FishData#color} and the Legendary/Mythic shine, so a tier looks the same on a fish, a sword or a crate.
 */
public final class HolyRarity {
    public static final List<String> TIERS = List.of("common", "uncommon", "rare", "epic", "legendary", "mythic", "cursed");
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
        best = Math.max(best, rank(nameTier(stack.getHoverName())));
        best = Math.max(best, FLOORS.getOrDefault(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), 0));
        if (RUNEFORGED) best = Math.max(best, Runeforged.rank(stack));
        return best <= 0 ? "" : TIERS.get(best);
    }

    /** Rank of a vanilla rarity component (0 common .. 3 epic), one step up when enchanted, never past Epic. */
    static int vanilla(Rarity rarity, boolean enchanted) {
        int rank = switch (rarity) { case UNCOMMON -> 1; case RARE -> 2; case EPIC -> 3; default -> 0; };
        return enchanted ? Math.min(3, rank + 1) : rank;
    }

    public static int rank(String tier) { return Math.max(0, TIERS.indexOf(tier)); }

    /** Items worth more than their vanilla rarity says (owner 2026-10-19): a floor, enchanting can still lift them. */
    static final java.util.Map<String, Integer> FLOORS = java.util.Map.ofEntries(
        java.util.Map.entry("minecraft:mace", 4), java.util.Map.entry("minecraft:trident", 4), java.util.Map.entry("minecraft:elytra", 4),
        java.util.Map.entry("minecraft:dragon_egg", 4), java.util.Map.entry("minecraft:nether_star", 4), java.util.Map.entry("minecraft:beacon", 4),
        java.util.Map.entry("minecraft:heavy_core", 4),
        java.util.Map.entry("minecraft:enchanted_golden_apple", 3), java.util.Map.entry("minecraft:totem_of_undying", 3), java.util.Map.entry("minecraft:conduit", 3),
        java.util.Map.entry("minecraft:dragon_head", 3), java.util.Map.entry("minecraft:end_crystal", 3),
        java.util.Map.entry("minecraft:netherite_ingot", 2), java.util.Map.entry("minecraft:netherite_upgrade_smithing_template", 2),
        java.util.Map.entry("minecraft:heart_of_the_sea", 2));

    // Runeforged's palette: quality and affix tier colours (ArpgItemQuality, ArpgAffixTier, ArpgWeaponHelper constants).
    static final int RF_MAGIC = 0x4AA3FF, RF_RARE = 0xFF7CFF, RF_LEGENDARY = 0xFF8C1A, RF_RED = 0xFF3333, RF_CURSED = 0x682A93, RF_RUNE = 0xCBA365;

    /** The tier a name's own colour (root or first coloured part) stands for: Runeforged colours by their tier, any other gold,
     *  orange or yellow name (lootbox tools, legendary loot) Legendary, red Runeforged names (cursed eggs and gems) Cursed. */
    static String nameTier(Component name) {
        if (name == null) return "";
        var color = name.getStyle().getColor();
        for (int i = 0; color == null && i < name.getSiblings().size(); i++) color = name.getSiblings().get(i).getStyle().getColor();
        if (color == null) return "";
        int rgb = color.getValue() & 0xFFFFFF;
        return switch (rgb) {
            case RF_MAGIC -> "uncommon"; case RF_RARE -> "rare"; case RF_RED, RF_CURSED -> "cursed"; case RF_RUNE -> "legendary";
            default -> golden(rgb) ? "legendary" : "";
        };
    }

    /** A tooltip text colour from Runeforged's palette in ours (T0 and alien red stay red, as Mythic). Other colours unchanged. */
    public static int remap(int rgb) {
        return switch (rgb & 0xFFFFFF) {
            case RF_MAGIC -> FishData.color("uncommon") & 0xFFFFFF; case RF_RARE -> FishData.color("rare") & 0xFFFFFF;
            case RF_LEGENDARY, RF_RUNE -> FishData.color("legendary") & 0xFFFFFF; case RF_RED -> FishData.color("mythic") & 0xFFFFFF;
            case RF_CURSED -> FishData.color("cursed") & 0xFFFFFF; default -> rgb & 0xFFFFFF;
        };
    }

    /** The component with every Runeforged-coloured part (also inside translation arguments) in our colours. */
    public static Component recolor(Component text) {
        var style = text.getStyle();
        if (style.getColor() != null) { int from = style.getColor().getValue(), to = remap(from); if (to != (from & 0xFFFFFF)) style = style.withColor(to); }
        var contents = text.getContents();
        if (contents instanceof net.minecraft.network.chat.contents.TranslatableContents translation && translation.getArgs().length > 0) {
            Object[] args = translation.getArgs().clone();
            for (int i = 0; i < args.length; i++) if (args[i] instanceof Component part) args[i] = recolor(part);
            contents = new net.minecraft.network.chat.contents.TranslatableContents(translation.getKey(), translation.getFallback(), args);
        }
        MutableComponent out = MutableComponent.create(contents).setStyle(style);
        for (var sibling : text.getSiblings()) out.append(recolor(sibling));
        return out;
    }

    static boolean golden(int rgb) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        if (max < 179 || max == 0 || (max - min) < 0.55f * max || max != r) return false;  // bright, saturated, red-led (orange to yellow)
        float degrees = 60f * (g - b) / (max - min);
        return degrees >= 25 && degrees <= 62;
    }

    /** The inventory outline: fish and crates from Uncommon, other items from Rare except blocks (Uncommon gear and blocks show their tier in the tooltip only). */
    public static boolean outlined(ItemStack stack, String tier) {
        if (tier.isEmpty()) return false;
        if (!FishData.rarity(stack).isEmpty() || !CrateStyle.rarity(stack).isEmpty()) return true;
        // A silhouette stroke looks wrong around 3D and see-through block models (beacon): blocks show their tier in the tooltip.
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem && !net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("holylois")) return false;
        return rank(tier) >= rank("rare");
    }

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
                var quality = com.ultimatearpg.item.ArpgWeaponHelper.peekVisualQuality(stack);
                // Rune-Cursed special weapons are crafted endgame gear: Legendary. Cursed and Alien-Cursed are the gamble: Cursed.
                if (quality == com.ultimatearpg.item.ArpgItemQuality.CURSED) return com.ultimatearpg.item.ArpgWeaponHelper.isRuneCursed(stack) ? 4 : 6;
                if (quality == com.ultimatearpg.item.ArpgItemQuality.LEGENDARY && com.ultimatearpg.item.ArpgWeaponHelper.isLegendaryPlus(stack)) return 5;
                return switch (quality) { case MAGIC -> 1; case RARE -> 2; case LEGENDARY -> 4; default -> 0; };
            } catch (RuntimeException | LinkageError error) {
                return 0;
            }
        }
    }
}
