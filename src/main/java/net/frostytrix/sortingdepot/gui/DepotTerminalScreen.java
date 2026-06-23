package net.frostytrix.sortingdepot.gui;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Depot Terminal: a read-only network dashboard listing each Linker Node's target,
 * filter, and fill, plus the Overflow Chest fill and the Controller's buffer count.
 */
public class DepotTerminalScreen extends AbstractContainerScreen<DepotTerminalMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "textures/gui/container/depot_terminal.png");
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int MAX_ROWS = 8;

    public DepotTerminalScreen(DepotTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 200, 180);
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

        TerminalSnapshot snapshot = this.menu.getSnapshot();
        int y = 18;
        if (snapshot.linkers().isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.frostyssortingdepot.terminal.empty"), 8, y, TEXT_COLOR, false);
        } else {
            int shown = Math.min(snapshot.linkers().size(), MAX_ROWS);
            for (int i = 0; i < shown; i++) {
                TerminalSnapshot.Entry entry = snapshot.linkers().get(i);
                graphics.text(this.font,
                        Component.literal(entry.target() + "  ·  " + entry.filter() + "  ·  " + entry.fill() + "%"),
                        8, y, TEXT_COLOR, false);
                y += 11;
            }
            if (snapshot.linkers().size() > MAX_ROWS) {
                graphics.text(this.font, Component.literal("+" + (snapshot.linkers().size() - MAX_ROWS) + " more"),
                        8, y, TEXT_COLOR, false);
            }
        }

        Component overflow = Component.translatable("gui.frostyssortingdepot.terminal.overflow",
                snapshot.hasOverflow() ? snapshot.overflowFill() + "%" : "-");
        Component buffer = Component.translatable("gui.frostyssortingdepot.terminal.buffer", snapshot.inputCount());
        graphics.text(this.font, overflow, 8, this.imageHeight - 32, TEXT_COLOR, false);
        graphics.text(this.font, buffer, 8, this.imageHeight - 22, TEXT_COLOR, false);
    }
}
