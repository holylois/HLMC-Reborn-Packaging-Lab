package holylois.boombox;

import net.minecraft.network.chat.Component;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * Shader list names (owner 2026-10-11): a short readable name plus how hard the pack works the PC, sorted light to heavy
 * (no colours, owner's call). Tiers are estimates from each pack's reputation.
 * Used by ShaderPackNamesMixin and ShaderPackListMixin; file names on disk never change.
 */
public final class ShaderPacks {
    private ShaderPacks() {}

    record Pack(Pattern file, String name, String strain) {}

    private static final Pack[] PACKS = {
        new Pack(Pattern.compile("(?i)^MakeUp-UltraFast"), "MakeUp Ultra Fast", "Lightest"),
        new Pack(Pattern.compile("(?i)^BSL_"), "BSL", "Light"),
        new Pack(Pattern.compile("(?i)^ComplementaryReimagined.*Euphoria"), "Complementary Reimagined + Euphoria", "Medium"),
        new Pack(Pattern.compile("(?i)^ComplementaryReimagined"), "Complementary Reimagined", "Medium"),
        new Pack(Pattern.compile("(?i)^Mellow"), "Mellow", "Heavy"),
        new Pack(Pattern.compile("(?i)^ComplementaryUnbound.*Euphoria"), "Complementary Unbound + Euphoria", "Heavy"),
        new Pack(Pattern.compile("(?i)^ComplementaryUnbound"), "Complementary Unbound", "Heavy"),
        new Pack(Pattern.compile("(?i)^Sildur's Vibrant.*Extreme"), "Sildur's Vibrant Extreme", "Very heavy"),
        new Pack(Pattern.compile("(?i)^Bliss"), "Bliss", "Heaviest"),
    };

    private static int index(String file) {
        for (int i = 0; i < PACKS.length; i++) if (PACKS[i].file().matcher(file).find()) return i;
        return -1;
    }

    /** Known packs light to heavy, unknown ones after them by name. */
    public static final Comparator<String> ORDER = Comparator.<String>comparingInt(f -> { int i = index(f); return i < 0 ? PACKS.length : i; })
        .thenComparing(String.CASE_INSENSITIVE_ORDER);

    /** The drawn name for a pack file in Iris's own style (grey while shaders are off), or the original when the pack is unknown. */
    public static Component display(String file, Component original) {
        int i = file == null ? -1 : index(file);
        if (i < 0) return original;
        Pack pack = PACKS[i];
        return Component.literal(pack.name() + " - " + pack.strain()).withStyle(original.getStyle());
    }
}
