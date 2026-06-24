package net.frostytrix.sortingdepot.gui;

import java.util.List;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Depot Terminal: a read-only network dashboard listing each Linker Node's target,
 * filter, and fill, plus the Overflow Chest fill and the Controller's buffer count. Scrolls when there
 * are more registered Linker Nodes than fit.
 */
public class DepotTerminalScreen extends AbstractContainerScreen<DepotTerminalMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "textures/gui/container/depot_terminal.png");
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int VISIBLE_ROWS = 12;
    private static final int ROW_HEIGHT = 10;
    private static final int LIST_TOP = 18;

    private int scrollOffset;

    public DepotTerminalScreen(DepotTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 180;
    }

    private int maxScroll() {
        return Math.max(0, this.menu.getSnapshot().linkers().size() - VISIBLE_ROWS);
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
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_COLOR, false);

        List<TerminalSnapshot.Entry> linkers = this.menu.getSnapshot().linkers();
        int rowWidth = this.imageWidth - 16; // 8px padding on each side
        if (linkers.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.frostyssortingdepot.terminal.empty"),
                    8, LIST_TOP, TEXT_COLOR, false);
        } else {
            scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
            int end = Math.min(linkers.size(), scrollOffset + VISIBLE_ROWS);
            int y = LIST_TOP;
            for (int i = scrollOffset; i < end; i++) {
                TerminalSnapshot.Entry entry = linkers.get(i);
                String full = entry.target() + "  ·  " + entry.filter() + "  ·  " + entry.fill() + "%";
                drawRow(graphics, full, y, rowWidth, mouseX, mouseY);
                y += ROW_HEIGHT;
            }
            // Scroll affordances, both arrows together at the bottom of the list.
            boolean hasAbove = scrollOffset > 0;
            boolean hasBelow = end < linkers.size();
            if (hasAbove || hasBelow) {
                StringBuilder indicator = new StringBuilder();
                if (hasAbove) {
                    indicator.append("▲");
                }
                if (hasBelow) {
                    if (hasAbove) {
                        indicator.append(' ');
                    }
                    indicator.append("▼ ").append(linkers.size() - end).append(" more");
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

    /**
     * Draws a row of terminal text, ellipsized to fit {@code rowWidth}; if the row was truncated and the
     * mouse hovers over it, the full text is shown as a tooltip. {@code mouseX}/{@code mouseY} are
     * GUI-local in renderLabels; the tooltip API wants screen-space, so {@link #leftPos}/{@link #topPos}
     * are added back.
     */
    private void drawRow(GuiGraphics graphics, String full, int y, int rowWidth, int mouseX, int mouseY) {
        String shown = fitWidth(full, rowWidth);
        graphics.drawString(this.font, Component.literal(shown), 8, y, TEXT_COLOR, false);
        if (!shown.equals(full)
                && mouseX >= 8 && mouseX < 8 + rowWidth
                && mouseY >= y && mouseY < y + ROW_HEIGHT - 1) {
            graphics.renderTooltip(this.font, Component.literal(full), mouseX + this.leftPos, mouseY + this.topPos);
        }
    }

    /** Truncates {@code s} with an ellipsis so it fits within {@code maxWidth} pixels. */
    private String fitWidth(String s, int maxWidth) {
        if (this.font.width(s) <= maxWidth) {
            return s;
        }
        return this.font.plainSubstrByWidth(s, maxWidth - this.font.width("…")) + "…";
    }
}
