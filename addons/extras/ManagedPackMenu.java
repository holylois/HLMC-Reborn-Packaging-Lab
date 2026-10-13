package holylois.boombox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import java.util.*;

/** An informational folder in the visible list, never a resource pack or part of the saved selection. */
public final class ManagedPackMenu {
    public static final String ID = "holylois:managed_resources";
    private static final List<String> NAMES = List.of("Eating Animation", "Eating Animation: Farmer's Delight",
        "Holy Lois: Fish Eating", "Fresh Animations", "Fresh Animations: Objects", "Just Expressions",
        "Fresh Animations: Player");

    static Component label(String key, Object... args) { return Component.translatable("holylois.resources." + key, args); }
    public static List<String> selected(PackRepository repository) {
        var ids = repository.getSelectedIds();
        return BundledPackOrder.IDS.reversed().stream().filter(ids::contains).toList();
    }
    static Component name(String id) { return Component.literal(NAMES.get(BundledPackOrder.IDS.indexOf(id))); }
    static Component description(String id) { return label("pack" + BundledPackOrder.IDS.indexOf(id)); }

    public static void attach(Screen screen, PackRepository repository, TransferableSelectionList list, String query) {
        var ids = selected(repository);
        if (ids.isEmpty()) return;
        String searchable = label("title").getString() + " " + label("summary", ids.size()).getString()
            + " " + ids.stream().map(id -> name(id).getString() + " " + description(id).getString()).reduce("", (a,b) -> a + " " + b);
        if (!searchable.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT).strip())) return;
        var entries = new ArrayList<>(list.children());
        entries.removeIf(entry -> ID.equals(entry.getPackId()));
        var heights = new IdentityHashMap<TransferableSelectionList.Entry, Integer>();
        entries.forEach(entry -> heights.put(entry, entry.getHeight()));
        int position = 0;
        while (position < entries.size() && !"vanilla".equals(entries.get(position).getPackId())) position++;
        var focused = list.getSelected();
        entries.add(position, new GroupEntry(list, screen, repository));
        list.replaceEntries(entries);
        // replaceEntries uses the default row height; retain vanilla's shorter column heading.
        heights.forEach((entry, height) -> entry.setHeight(height));
        list.updateSizeAndPosition(list.getWidth(), list.getHeight(), list.getX(), list.getY());
        if (focused != null && entries.contains(focused)) list.setSelected(focused);
    }

    public static final class GroupEntry extends TransferableSelectionList.Entry {
        private final Screen parent;
        private final PackRepository repository;
        GroupEntry(TransferableSelectionList list, Screen parent, PackRepository repository) {
            list.super(); this.parent = parent; this.repository = repository;
        }
        @Override public String getPackId() { return ID; }
        @Override public Component getNarration() { return label("title").copy().append(". ").append(label("summary", selected(repository).size())).append(". ").append(label("open")); }
        private void open() { Minecraft.getInstance().gui.setScreen(new ManagedResourcesScreen(parent, repository)); }
        @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 1 || !isMouseOver(event.x(), event.y())) return false;
            open(); return true;
        }
        @Override public boolean keyPressed(KeyEvent event) {
            if (!event.isConfirmation()) return false;
            open(); return true;
        }
        @Override public void extractContent(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean hovered, float tick) {
            var font = Minecraft.getInstance().font;
            int x = getContentX(), y = getContentY();
            // Folder and padlock use native shapes so they stay crisp at every GUI scale.
            Ui.box(g, x+1, y+5, 15, 6, Ui.GOLD_SOFT, Ui.GOLD);
            Ui.box(g, x, y+10, 31, 21, Ui.CONTROL, hovered || isFocused() ? Ui.GOLD : Ui.LINE_STRONG);
            g.fill(x+21, y+17, x+28, y+24, Ui.GOLD);
            g.fill(x+22, y+13, x+27, y+18, Ui.GOLD);
            g.fill(x+23, y+14, x+26, y+17, Ui.CONTROL);
            g.text(font, font.plainSubstrByWidth(label("title").getString(), getContentWidth()-35), x+34, y+1, Ui.TEXT, true);
            g.text(font, label("summary", selected(repository).size()), x+34, y+12, Ui.MUTED, true);
            g.text(font, label("open"), x+34, y+22, hovered || isFocused() ? Ui.GOLD : Ui.MUTED, true);
        }
    }
}
