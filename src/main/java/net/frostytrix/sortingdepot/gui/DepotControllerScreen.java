package net.frostytrix.sortingdepot.gui;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Depot Controller. Compact container showing the input-buffer slot.
 */
public class DepotControllerScreen extends AbstractContainerScreen<DepotControllerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "textures/gui/container/depot_controller.png");

    public DepotControllerScreen(DepotControllerMenu menu, Inventory inventory, Component title) {
        // Compact 176×133 window (single input slot), not a full chest-height container.
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 133;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }
}
