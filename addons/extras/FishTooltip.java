package holylois.boombox;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Every fish shows its colors (owner 2026-10-11): right under the name a row of swatches from the fish's own texture, and for
 * Fish of Thieves the variant name (Charcoal, Ruby...) in its color instead of the mod's gray line. The order then reads
 * name, look, rarity, weight, effects, catch details. Colors come from make-fish-art.py (assets/holylois/fish_palettes.json).
 */
public final class FishTooltip {
    private FishTooltip() {}
    private static JsonObject palettes;

    record Look(int[] palette, int color, String nameKey) {}

    static void register() {
        try (var in = FishTooltip.class.getResourceAsStream("/assets/holylois/fish_palettes.json")) {
            if (in == null) return;
            palettes = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception error) { Boombox.LOG.warn("Cannot read fish palettes", error); return; }
        ItemTooltipCallback.EVENT.register((stack, context, flags, lines) -> decorate(stack, lines));
    }

    static void decorate(ItemStack stack, List<Component> lines) {
        if (lines.isEmpty()) return;
        var look = look(stack);
        if (look == null) return;
        MutableComponent row = Component.empty();
        for (int rgb : look.palette()) row.append(Component.literal("■").withStyle(s -> s.withColor(rgb)));
        if (look.nameKey() != null) {
            String name = Component.translatable(look.nameKey()).getString();
            // Fish of Thieves' own gray variant line goes; the name moves up here in color.
            for (int i = lines.size() - 1; i >= 1; i--) if (lines.get(i).getString().equals(name)) lines.remove(i);
            row.append(" ").append(Component.translatable(look.nameKey()).withStyle(s -> s.withColor(look.color())));
        }
        lines.add(1, row);
    }

    static Look look(ItemStack stack) {
        if (palettes == null || stack.isEmpty()) return null;
        var entry = palettes.getAsJsonObject(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (entry == null) return null;
        if (entry.has("variant_key")) {
            var data = stack.get(DataComponents.CUSTOM_DATA);
            String variant = data == null ? "" : data.copyTag().getStringOr(entry.get("variant_key").getAsString(), "");
            var variants = entry.getAsJsonObject("variants");
            if (variants.has(variant)) return of(variants.getAsJsonObject(variant), true);
        }
        return entry.has("palette") ? of(entry, false) : null;
    }

    private static Look of(JsonObject json, boolean named) {
        var list = json.getAsJsonArray("palette");
        int[] palette = new int[list.size()];
        for (int i = 0; i < palette.length; i++) palette[i] = Integer.parseInt(list.get(i).getAsString().substring(1), 16);
        int color = Integer.parseInt(json.get("color").getAsString().substring(1), 16);
        return new Look(palette, color, named ? json.get("name_key").getAsString() : null);
    }
}
