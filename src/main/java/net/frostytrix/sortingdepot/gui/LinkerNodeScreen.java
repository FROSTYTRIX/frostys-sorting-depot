package net.frostytrix.sortingdepot.gui;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "textures/gui/container/linker_node.png");

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
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        if (this.menu.isLinked()) {
            int x = this.leftPos;
            int y = this.topPos;
            boolean hovered = mouseX >= x + BTN_X && mouseX < x + BTN_X + BTN_W
                    && mouseY >= y + BTN_Y && mouseY < y + BTN_Y + BTN_H;
            graphics.fill(x + BTN_X, y + BTN_Y, x + BTN_X + BTN_W, y + BTN_Y + BTN_H,
                    hovered ? BTN_HOVER : BTN);
            Component label = Component.translatable("gui.frostyssortingdepot.linker_node.unlink");
            int textX = x + BTN_X + (BTN_W - this.font.width(label)) / 2;
            graphics.drawString(this.font, label, textX, y + BTN_Y + (BTN_H - 8) / 2, TEXT_WHITE, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component priority = Component.translatable(
                "item.frostyssortingdepot.priority_stamp.value", this.menu.getNode().getPriority());
        graphics.drawString(this.font, priority, 8, 20, 0xFF404040, false);

        Component status = Component.translatable(this.menu.isLinked()
                ? "gui.frostyssortingdepot.linker_node.linked"
                : "gui.frostyssortingdepot.linker_node.unlinked");
        graphics.drawString(this.font, status, 8, 31, this.menu.isLinked() ? 0xFF3A7A3A : 0xFF7A3A3A, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.isLinked()) {
            int rx = (int) mouseX - this.leftPos;
            int ry = (int) mouseY - this.topPos;
            if (rx >= BTN_X && rx < BTN_X + BTN_W && ry >= BTN_Y && ry < BTN_Y + BTN_H) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, LinkerNodeMenu.BTN_UNLINK);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
