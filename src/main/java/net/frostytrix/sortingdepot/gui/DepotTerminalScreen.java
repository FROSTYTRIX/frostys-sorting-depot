package net.frostytrix.sortingdepot.gui;

import java.util.List;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Depot Terminal: a read-only network dashboard listing each Linker Node's target,
 * filter, and fill, plus the Overflow Chest fill and the Controller's buffer count. Rows that are too
 * wide wrap onto extra lines (keeping the row's left offset, with the fill % trailing at the end); the
 * list scrolls per-entry when there are more rows than fit.
 */
public class DepotTerminalScreen extends AbstractContainerScreen<DepotTerminalMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "textures/gui/container/depot_terminal.png");
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ROW_HEIGHT = 10;
    private static final int LIST_TOP = 18;
    private static final int LIST_BOTTOM_MARGIN = 36; // reserved for the overflow + buffer lines

    private int scrollOffset; // index of the first entry shown

    public DepotTerminalScreen(DepotTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 180;
    }

    /** Bottom of the entry-drawing area; one row is held back for the scroll indicator. */
    private int entryAreaBottom() {
        return this.imageHeight - LIST_BOTTOM_MARGIN - ROW_HEIGHT;
    }

    private int rowWidth() {
        return this.imageWidth - 16; // 8px padding on each side
    }

    /** The wrapped display lines for one entry: {@code target · filter · fill%}, wrapped to the row width. */
    private List<FormattedCharSequence> entryLines(TerminalSnapshot.Entry entry) {
        String full = entry.target() + "  ·  " + entry.filter() + "  ·  " + entry.fill() + "%";
        return this.font.split(Component.literal(full), rowWidth());
    }

    /** The largest scroll offset that still fills the list from the bottom (so every entry stays reachable). */
    private int maxScroll() {
        List<TerminalSnapshot.Entry> linkers = this.menu.getSnapshot().linkers();
        int capacity = entryAreaBottom() - LIST_TOP;
        int used = 0;
        for (int i = linkers.size() - 1; i >= 0; i--) {
            used += entryLines(linkers.get(i)).size() * ROW_HEIGHT;
            if (used > capacity) {
                return i + 1;
            }
        }
        return 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll() > 0) {
            scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_COLOR, false);

        List<TerminalSnapshot.Entry> linkers = this.menu.getSnapshot().linkers();
        if (linkers.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.frostyssortingdepot.terminal.empty"),
                    8, LIST_TOP, TEXT_COLOR, false);
        } else {
            scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
            int bottom = entryAreaBottom();
            int y = LIST_TOP;
            int i = scrollOffset;
            for (; i < linkers.size(); i++) {
                List<FormattedCharSequence> lines = entryLines(linkers.get(i));
                if (y + lines.size() * ROW_HEIGHT > bottom) {
                    break; // next entry would overflow the list area
                }
                for (FormattedCharSequence line : lines) {
                    graphics.drawString(this.font, line, 8, y, TEXT_COLOR, false);
                    y += ROW_HEIGHT;
                }
            }
            // Scroll affordances, both arrows together below the last drawn entry.
            boolean hasAbove = scrollOffset > 0;
            boolean hasBelow = i < linkers.size();
            if (hasAbove || hasBelow) {
                StringBuilder indicator = new StringBuilder();
                if (hasAbove) {
                    indicator.append("▲");
                }
                if (hasBelow) {
                    if (hasAbove) {
                        indicator.append(' ');
                    }
                    indicator.append("▼ ").append(linkers.size() - i).append(" more");
                }
                graphics.drawString(this.font, Component.literal(indicator.toString()), 8, y, TEXT_COLOR, false);
            }
        }

        Component overflow = Component.translatable("gui.frostyssortingdepot.terminal.overflow",
                this.menu.getSnapshot().hasOverflow() ? this.menu.getSnapshot().overflowFill() + "%" : "-");
        Component buffer = Component.translatable("gui.frostyssortingdepot.terminal.buffer",
                this.menu.getSnapshot().inputCount());
        graphics.drawString(this.font, overflow, 8, this.imageHeight - 32, TEXT_COLOR, false);
        graphics.drawString(this.font, buffer, 8, this.imageHeight - 22, TEXT_COLOR, false);
    }
}
