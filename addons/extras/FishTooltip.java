package holylois.boombox;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Hide color swatches and variant labels while retaining variant textures and palette data for future use. */
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
        if (look.nameKey() != null) {
            String name = Component.translatable(look.nameKey()).getString();
            // Hide Fish of Thieves' own variant line, too.
            for (int i = lines.size() - 1; i >= 1; i--) if (lines.get(i).getString().equals(name)) lines.remove(i);
        }
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
