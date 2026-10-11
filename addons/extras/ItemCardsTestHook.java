package holylois.boombox;

/** Read-only view of item cards for the client gametests. */
public final class ItemCardsTestHook {
    private ItemCardsTestHook() {}

    public static int shown() { return ItemCards.cardsShown; }
}
