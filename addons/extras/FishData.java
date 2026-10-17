package holylois.boombox;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.Locale;

/**
 * What a weighed fish is (custom_data holylois_fish written by Legends.weigh): rarity, size, weight. Safe on both sides;
 * the client-only drawing lives in FishLook (a client class here once crashed the server's cutting board).
 */
public final class FishData {
    private FishData() {}

    static CompoundTag tag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        return data.copyTag().getCompound(Legends.FISH_KEY).orElse(null);
    }

    public static String rarity(ItemStack stack) {
        var fish = tag(stack);
        return fish == null ? "" : fish.getStringOr("rarity", "");
    }

    /** Weight in kg, 0 for a fish that was never weighed. */
    public static double kilograms(ItemStack stack) {
        var fish = tag(stack);
        return fish == null ? 0 : fish.getDoubleOr("kg", 0);
    }

    public static boolean shiny(ItemStack stack) {
        var fish = tag(stack);
        return fish != null && fish.getBooleanOr("shiny", false);
    }

    /** Size 0..1 by roll (Mythic 1.5..2.5); older trophies have none, then by rarity. */
    static double size(CompoundTag fish) {
        double size = fish.getDoubleOr("size", -1);
        if (size >= 0) return size;
        return switch (fish.getStringOr("rarity", "")) { case "rare" -> 0.8; case "epic" -> 0.93; case "legendary" -> 0.98; case "mythic" -> 2; default -> -1; };
    }

    /** Size 0..1 gives 1x to 1.7x; a Mythic (size 1.5..2.5) is absurd, capped at 3.5x. */
    static float scale(String rarity, double size) {
        if (size < 0) size = switch (rarity) { case "rare" -> 0.8; case "epic" -> 0.93; case "legendary" -> 0.98; case "mythic" -> 2; default -> -1; };
        if (size < 0) return rarity.equals("uncommon") ? 1.1f : 1;
        return (float) Math.min(3.5, 1 + 0.7 * size * size);
    }

    /** The heaviest a fish can weigh (owner 2026-10-16): a colossal Mythic at lottery odds. */
    public static final double MAX_KG = 255;

    /**
     * How big a weighed fish looks, from its kg. Up to 20 kg a square root, so 2 kg, 10 kg and 20 kg read clearly apart:
     * 1 kg 1.45x, 3 kg 1.85x, 10 kg 2.6x, 20 kg 3.4x. Past that it keeps growing a little with every doubling (owner
     * 2026-10-16, a 20 kg Legendary must look clearly smaller than an 80 kg Mythic): 40 kg 4.3x, 80 kg 5.2x, 255 kg 6.7x.
     */
    public static float scaleKg(double kg) {
        if (!(kg > 0)) return 1;
        kg = Math.min(kg, MAX_KG);
        double s = 0.9 + 0.55 * Math.sqrt(Math.min(kg, 20));
        if (kg > 20) s += 0.9 * Math.log(kg / 20) / Math.log(2);
        return (float) Math.max(1, s);
    }

    /** In a frame or on the cutting board the block bounds the fish: as in the world up to 2x, then squeezed so the heaviest reaches 3x. */
    public static float frameScale(float world) {
        return world <= 2 ? world : 2 + (world - 2) / (scaleKg(MAX_KG) - 2);
    }

    /** From mid Epic (10 kg) a fish is carried in both arms in front of the body instead of one hand. */
    public static final double TWO_HANDED_KG = 10;

    /** A trophy (Rare and up): it has a weight. */
    public static boolean weighed(ItemStack stack) {
        var fish = tag(stack);
        return fish != null && fish.contains("kg");
    }

    public static boolean twoHanded(ItemStack stack) {
        var fish = tag(stack);
        return fish != null && fish.getDoubleOr("kg", 0) >= TWO_HANDED_KG;
    }

    /**
     * Ticks to eat a trophy fish (owner 2026-10-11): Rare 2 s, Epic 2.6 s, Legendary 3.2 s, Mythic 3.2 s plus 1 s per 20 kg,
     * 8 s at most. Fillets and plain fish keep the normal time.
     */
    public static int eatTicks(ItemStack stack, int normal) {
        String rarity = rarity(stack);
        if (rarity.isEmpty() || !weighed(stack) || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_slice")) return normal;
        double seconds = switch (rarity) {
            case "rare" -> 2.0; case "epic" -> 2.6; case "legendary" -> 3.2;
            case "mythic" -> 3.2 + kilograms(stack) / 20; default -> 0; };
        return (int) Math.round(Math.max(normal / 20.0, Math.min(8, seconds)) * 20);
    }

    /** Fonts that draw like the default one; the client gives their glyphs a moving shine (ShineText). */
    public static final net.minecraft.resources.Identifier SHINE_LEGENDARY = net.minecraft.resources.Identifier.fromNamespaceAndPath("holylois", "legendary"),
        SHINE_MYTHIC = net.minecraft.resources.Identifier.fromNamespaceAndPath("holylois", "mythic");

    /** The style with the rarity's shine font (Legendary and Mythic), else unchanged. */
    public static net.minecraft.network.chat.Style shine(net.minecraft.network.chat.Style style, String rarity) {
        var id = switch (rarity) { case "legendary" -> SHINE_LEGENDARY; case "mythic" -> SHINE_MYTHIC; default -> null; };
        return id == null ? style : style.withFont(new net.minecraft.network.chat.FontDescription.Resource(id));
    }

    /**
     * Rarity colour (owner 2026-10-11): the familiar green/blue/purple/gold/red, tuned to read at 6.7:1 or better on the dark
     * tooltip background (WoW's own blue and purple are only 4.2:1). Common is F2F2F2 where a tier needs a colour.
     */
    public static int color(String rarity) {
        return switch (rarity) {
            case "uncommon" -> 0xFF5BE37D;
            case "rare" -> 0xFF4AA8FF;
            case "epic" -> 0xFFC27CFF;
            case "legendary" -> 0xFFFFB43D;
            case "mythic" -> 0xFFFF5A5A;
            default -> 0;
        };
    }

    /** Slices a trophy gives in all, by weight: 2 plus one per 1.5 kg (3 kg 4, 10 kg 9, 40 kg 29), 64 at most. 0 for other fish. */
    public static int slices(ItemStack stack) {
        var fish = tag(stack);
        if (fish == null || !fish.contains("kg")) return 0;
        return (int) Math.max(2, Math.min(64, Math.round(2 + fish.getDoubleOr("kg", 0) / 1.5)));
    }

    /** Slices per knife cut: a big fish takes several cuts and loses health (a damage bar) with each one. */
    public static final int PER_CUT = 4;

    public static int cuts(ItemStack stack) { return Math.max(1, (slices(stack) + PER_CUT - 1) / PER_CUT); }

    /** Slices from the next cut (the last cut gets what is left). */
    public static int slicesThisCut(ItemStack stack) {
        int done = Math.max(0, stack.getDamageValue()), total = slices(stack);
        return done + 1 >= cuts(stack) ? Math.max(1, total - PER_CUT * done) : PER_CUT;
    }

    /** A fillet cut from a trophy: it remembers rarity, shine and species (not weight, so equal fillets stack). */
    public static boolean fillet(ItemStack stack) {
        var fish = tag(stack);
        return fish != null && fish.getBooleanOr("fillet", false);
    }

    /** "0.85", "4.2", "12", "1.2k": fits in the corner of a slot. */
    public static String shortWeight(double kg) {
        if (kg < 10) return String.format(Locale.ROOT, kg < 1 ? "%.2f" : "%.1f", kg);
        if (kg < 1000) return String.valueOf(Math.round(kg));
        return String.format(Locale.ROOT, "%.1fk", kg / 1000);
    }

    /** The weight label for a slot, or null (Common and Uncommon fish have no weight). */
    public static String label(ItemStack stack) {
        var fish = tag(stack);
        if (fish == null || !fish.contains("kg")) return null;
        return shortWeight(fish.getDoubleOr("kg", 0));
    }
}
