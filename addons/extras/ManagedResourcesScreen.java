package holylois.boombox;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

/** Read-only pack contents, in the same top-to-bottom priority order as Minecraft's selection menu. */
public final class ManagedResourcesScreen extends TravelScreen {
    private record Row(Component title, List<FormattedCharSequence> description, int y, int height) {}
    private final Screen parent;
    private final List<String> ids;
    private final List<Row> rows = new ArrayList<>();
    private List<FormattedCharSequence> intro;
    private int bodyY, bodyBottom, scroll, maxScroll, headingY;

    public ManagedResourcesScreen(Screen parent, PackRepository repository) {
        super("managed_resources_title"); this.parent = parent; ids = ManagedPackMenu.selected(repository);
    }
    public List<String> packIds() { return ids; }
    @Override protected void init() {
        panelWidth = Math.min(500, width-24); panelHeight = Math.min(380, height-24);
        left = (width-panelWidth)/2; top = (height-panelHeight)/2;
        intro = font.split(ManagedPackMenu.label("explanation"), panelWidth-32);
        headingY = top+34+intro.size()*11+8;
        bodyY = headingY+17; bodyBottom = top+panelHeight-44;
        rows.clear(); int y = 0;
        for (int i=0; i<ids.size(); i++) {
            String id = ids.get(i);
            var title = Component.literal((i+1) + ". ").append(ManagedPackMenu.name(id));
            var description = font.split(ManagedPackMenu.description(id), panelWidth-50);
            int rowHeight = 12+description.size()*11+9;
            rows.add(new Row(title, description, y, rowHeight)); y += rowHeight+4;
        }
        maxScroll = Math.max(0, y-4-(bodyBottom-bodyY)); scroll = Math.min(scroll, maxScroll);
        buttonText(left+panelWidth-92, top+panelHeight-32, 76, ManagedPackMenu.label("back"), this::onClose);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (maxScroll == 0 || x<left+16 || x>left+panelWidth-16 || y<bodyY || y>bodyBottom) return false;
        scroll = Math.clamp(scroll-(int)(vertical*28), 0, maxScroll); return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (maxScroll>0 && (event.isUp() || event.isDown())) {
            scroll = Math.clamp(scroll+(event.isDown()?28:-28), 0, maxScroll); return true;
        }
        return super.keyPressed(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float tick) {
        super.extractRenderState(g, x, y, tick);
        int iy = top+34;
        for (var line : intro) { g.text(font, line, left+16, iy, Ui.MUTED, false); iy+=11; }
        g.text(font, ManagedPackMenu.label("priority"), left+16, headingY, Ui.GOLD, false);
        g.enableScissor(left+16, bodyY, left+panelWidth-16, bodyBottom);
        for (var row : rows) {
            int ry = bodyY+row.y()-scroll;
            Ui.box(g, left+16, ry, panelWidth-38, row.height(), Ui.RAISED, Ui.LINE);
            g.text(font, row.title(), left+24, ry+7, Ui.TEXT, false);
            int dy=ry+19;
            for (var line : row.description()) { g.text(font, line, left+24, dy, Ui.MUTED, false); dy+=11; }
        }
        g.disableScissor();
        if (maxScroll>0) {
            int h=bodyBottom-bodyY, thumb=Math.max(16,h*h/(h+maxScroll));
            int sy=bodyY+(h-thumb)*scroll/maxScroll;
            g.fill(left+panelWidth-19, bodyY, left+panelWidth-17, bodyBottom, Ui.LINE);
            g.fill(left+panelWidth-19, sy, left+panelWidth-17, sy+thumb, Ui.MUTED);
        }
    }
}
