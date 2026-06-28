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
 * Screen for the Linker Node menu. Standard 176×166 container with a single Filter Card slot.
 * When the node is registered to a Controller, an <b>Unlink</b> button appears next to the status
 * label so the player can de-register the node without breaking the block.
 *
 * <p>Client-only by reference: it is registered solely from the client-dist {@code RegisterMenuScreensEvent}
 * handler, so it never classloads on a dedicated server — no {@code @OnlyIn} needed.
 */
public class LinkerNodeScreen extends AbstractContainerScreen<LinkerNodeMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "textures/gui/container/linker_node.png");

    // Unlink button (relative to leftPos/topPos). Sits to the right of the linked/unlinked status label.
    private static final int BTN_X = 110;
    private static final int BTN_Y = 28;
    private static final int BTN_W = 56;
    private static final int BTN_H = 14;
    private static final int BTN = 0xFF8A4A4A;
    private static final int BTN_HOVER = 0xFFA85A5A;
    private static final int TEXT_WHITE = 0xFFFFFFFF;

    public LinkerNodeScreen(LinkerNodeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        if (this.menu.isLinked()) {
            boolean hovered = mouseX >= x + BTN_X && mouseX < x + BTN_X + BTN_W
                    && mouseY >= y + BTN_Y && mouseY < y + BTN_Y + BTN_H;
            graphics.fill(x + BTN_X, y + BTN_Y, x + BTN_X + BTN_W, y + BTN_Y + BTN_H,
                    hovered ? BTN_HOVER : BTN);
            graphics.centeredText(this.font, Component.translatable("gui.frostyssortingdepot.linker_node.unlink"),
                    x + BTN_X + BTN_W / 2, y + BTN_Y + (BTN_H - 8) / 2, TEXT_WHITE);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component priority = Component.translatable(
                "item.frostyssortingdepot.priority_stamp.value", this.menu.getNode().getPriority());
        graphics.text(this.font, priority, 8, 20, 0xFF404040, false);

        Component status = Component.translatable(this.menu.isLinked()
                ? "gui.frostyssortingdepot.linker_node.linked"
                : "gui.frostyssortingdepot.linker_node.unlinked");
        graphics.text(this.font, status, 8, 31, this.menu.isLinked() ? 0xFF3A7A3A : 0xFF7A3A3A, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && this.menu.isLinked()) {
            int rx = (int) event.x() - this.leftPos;
            int ry = (int) event.y() - this.topPos;
            if (rx >= BTN_X && rx < BTN_X + BTN_W && ry >= BTN_Y && ry < BTN_Y + BTN_H) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, LinkerNodeMenu.BTN_UNLINK);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
