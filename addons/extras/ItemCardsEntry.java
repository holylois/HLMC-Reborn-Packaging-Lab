package holylois.boombox;

import net.minecraft.world.item.ItemStack;

/** Public door to ItemCards for the mixin package. */
public final class ItemCardsEntry {
    private ItemCardsEntry() {}

    public static void pickedUp(ItemStack stack) { ItemCards.pickedUp(stack); }
}
