package holylois.boombox;

import com.ultimatearpg.item.ArpgItemQuality;
import com.ultimatearpg.item.ArpgWeaponHelper;
import com.ultimatearpg.monster.ArpgGeneratedWeaponService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Dungeon shelves (invisible item frames in structures, owner 2026-10-19): gear taken from one is now and then a Runeforged
 * piece, rolled by Runeforged itself (tier + bonuses), so it shows up in our Uncommon/Rare/Legendary colours (HolyRarity).
 */
public final class RuneforgedFrameLoot {
    static final double CHANCE = 0.12;
    private RuneforgedFrameLoot() {}

    /** Rolls once, on the server, just before the frame drops its item. */
    public static void roll(ItemStack stack, RandomSource random) {
        if (stack.isEmpty() || stack.getCount() != 1 || !gear(stack) || random.nextDouble() >= CHANCE) return;
        try {
            if (!ArpgWeaponHelper.peekQuality(stack).isCommon()) return;
            ArpgGeneratedWeaponService.applyQualityAndRollAffixes(stack, quality(random.nextDouble()), random);
        } catch (RuntimeException | LinkageError error) {
            Boombox.LOG.warn("Runeforged frame loot roll failed for {}", stack, error);
        }
    }

    static ArpgItemQuality quality(double roll) {
        return roll < 0.07 ? ArpgItemQuality.LEGENDARY : roll < 0.35 ? ArpgItemQuality.RARE : ArpgItemQuality.MAGIC;
    }

    static boolean gear(ItemStack stack) {
        return stack.has(DataComponents.WEAPON) || stack.has(DataComponents.TOOL) || stack.has(DataComponents.EQUIPPABLE) && stack.isDamageableItem();
    }
}
