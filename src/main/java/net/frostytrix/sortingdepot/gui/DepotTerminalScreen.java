package net.frostytrix.sortingdepot.gui;

import java.util.List;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Depot Terminal: a read-only network dashboard listing each Linker Node's target,
 * filter, and fill, plus the Overflow Chest fill and the Controller's buffer count. Scrolls when there
 * are more registered Linker Nodes than fit.
 */
public class DepotTerminalScreen extends AbstractContainerScreen<DepotTerminalMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "textures/gui/container/depot_terminal.png");
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int VISIBLE_ROWS = 12;
    private static final int ROW_HEIGHT = 10;
    private static final int LIST_TOP = 18;

    private int scrollOffset;

    public DepotTerminalScreen(DepotTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 200, 180);
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT_COLOR, false);

        List<TerminalSnapshot.Entry> linkers = this.menu.getSnapshot().linkers();
        if (linkers.isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.frostyssortingdepot.terminal.empty"),
                    8, LIST_TOP, TEXT_COLOR, false);
        } else {
            scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
            int end = Math.min(linkers.size(), scrollOffset + VISIBLE_ROWS);
            int y = LIST_TOP;
            for (int i = scrollOffset; i < end; i++) {
                TerminalSnapshot.Entry entry = linkers.get(i);
                graphics.text(this.font,
                        Component.literal(entry.target() + "  ·  " + entry.filter() + "  ·  " + entry.fill() + "%"),
                        8, y, TEXT_COLOR, false);
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
                graphics.text(this.font, Component.literal(indicator.toString()), 8, y, TEXT_COLOR, false);
            }
        }

        Component overflow = Component.translatable("gui.frostyssortingdepot.terminal.overflow",
                this.menu.getSnapshot().hasOverflow() ? this.menu.getSnapshot().overflowFill() + "%" : "-");
        Component buffer = Component.translatable("gui.frostyssortingdepot.terminal.buffer",
                this.menu.getSnapshot().inputCount());
        graphics.text(this.font, overflow, 8, this.imageHeight - 32, TEXT_COLOR, false);
        graphics.text(this.font, buffer, 8, this.imageHeight - 22, TEXT_COLOR, false);
    }
}
