package net.frostytrix.sortingdepot.gui;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Depot Controller. Compact container showing the input-buffer slot, plus a toggle button
 * for the distribution mode (first-match vs round-robin).
 */
public class DepotControllerScreen extends AbstractContainerScreen<DepotControllerMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "textures/gui/container/depot_controller.png");

    // Distribution-mode toggle button (relative to leftPos/topPos).
    private static final int BTN_X = 8;
    private static final int BTN_Y = 20;
    private static final int BTN_W = 64;
    private static final int BTN_H = 16;

    private static final int BTN = 0xFF6E6E6E;
    private static final int BTN_ACTIVE = 0xFF4E7A4E;
    private static final int TEXT_WHITE = 0xFFFFFFFF;

    public DepotControllerScreen(DepotControllerMenu menu, Inventory inventory, Component title) {
        // Compact 176×133 window (single input slot), not a full chest-height container.
        super(menu, inventory, title, 176, 133);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        boolean rr = this.menu.isRoundRobin();
        graphics.fill(x + BTN_X, y + BTN_Y, x + BTN_X + BTN_W, y + BTN_Y + BTN_H, rr ? BTN_ACTIVE : BTN);
        Component label = Component.translatable(rr
                ? "gui.frostyssortingdepot.controller.round_robin"
                : "gui.frostyssortingdepot.controller.first_match");
        graphics.centeredText(this.font, label, x + BTN_X + BTN_W / 2, y + BTN_Y + (BTN_H - 8) / 2, TEXT_WHITE);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int rx = (int) event.x() - this.leftPos;
            int ry = (int) event.y() - this.topPos;
            if (rx >= BTN_X && rx < BTN_X + BTN_W && ry >= BTN_Y && ry < BTN_Y + BTN_H) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        DepotControllerMenu.BTN_TOGGLE_ROUND_ROBIN);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
