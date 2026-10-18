package holylois.boombox;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Legendary Tooltips frames in our tiers (client only): tier-coloured border; gold spikes only for Legendary, our own red Mythic and
 * purple Cursed frames (textures/gui/tooltip_frames.png, original art from make-frames.py). Index -1 is the mod's standard border,
 * which draws gold spikes too, so everything below Legendary gets -2 (plain). Used by both the frame lookup and the colour lookup.
 */
public final class TierFrames {
    private static final Identifier BORDERS = Identifier.fromNamespaceAndPath("legendarytooltips", "textures/gui/tooltip_borders.png"),
        HOLY = Identifier.fromNamespaceAndPath("holylois", "textures/gui/tooltip_frames.png");
    private TierFrames() {}

    /** Dark tooltip backgrounds: vanilla for most, warm brown for Legendary (the mod's gold frame), a red or violet tint for Mythic and Cursed. */
    static int background(String tier) {
        return switch (tier) { case "legendary" -> 0xF0160A00; case "mythic" -> 0xF01A0606; case "cursed" -> 0xF0120620; default -> 0xF0100010; };
    }

    public static FrameDefinition adjust(ItemStack stack, FrameDefinition old) {
        if (old == null) return null;
        String tier = HolyRarity.of(stack);
        boolean ours = tier.equals("mythic") || tier.equals("cursed"), fancy = ours || tier.equals("legendary"), hadFrame = old.index() >= 0;
        int index = ours ? (tier.equals("mythic") ? 0 : 1) : fancy ? (hadFrame ? old.index() : 0) : -2;
        int color = FishData.color(tier);
        if (index == old.index() && color == 0) return old;
        boolean defaults = ours || fancy && !hadFrame;
        // The standard border (-1) gets its background later from the call site; once we switch it, give the background here.
        boolean ownBackground = ours || !hadFrame;
        int top = background(tier), bottom = (top & 0x00FFFFFF) | 0xE8000000;
        return new FrameDefinition(ours ? HOLY : old.resource() != null ? old.resource() : BORDERS, index,
            color != 0 ? () -> color : old.startBorder(), color != 0 ? () -> color : old.endBorder(),
            ownBackground ? () -> top : old.startBackground(), ownBackground ? () -> bottom : old.endBackground(),
            old.source(), old.priority(),
            defaults ? LegendaryTooltipsConfig.DEFAULT_FRAME_WIDTH : old.frameWidth(), defaults ? LegendaryTooltipsConfig.DEFAULT_PART_SIZE : old.partSize(),
            defaults ? LegendaryTooltipsConfig.DEFAULT_PART_OFFSET : old.partOffset(), defaults ? LegendaryTooltipsConfig.DEFAULT_CORNER_OFFSET : old.cornerOffset());
    }
}
