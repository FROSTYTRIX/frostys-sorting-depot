package net.frostytrix.sortingdepot.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.blockentity.LinkerNodeBlockEntity;
import net.frostytrix.sortingdepot.network.RenameLinkerNodePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Linker Node menu. Standard 176×166 container with a single Filter Card slot.
 * When the node is registered to a Controller, an <b>Unlink</b> button appears next to the status
 * label so the player can de-register the node without breaking the block. A name field at the top
 * lets the player give the node a custom label that shows in the Depot Terminal.
 *
 * <p>Client-only by reference: it is registered solely from the client-dist {@code RegisterMenuScreensEvent}
 * handler, so it never classloads on a dedicated server — no {@code @OnlyIn} needed.
 */
public class LinkerNodeScreen extends AbstractContainerScreen<LinkerNodeMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "textures/gui/container/linker_node.png");

    private static final int BTN_X = 110;
    private static final int BTN_Y = 28;
    private static final int BTN_W = 56;
    private static final int BTN_H = 14;
    private static final int BTN = 0xFF8A4A4A;
    private static final int BTN_HOVER = 0xFFA85A5A;
    private static final int TEXT_WHITE = 0xFFFFFFFF;

    private static final int NAME_X = 8;
    private static final int NAME_Y = 6;
    private static final int NAME_W = 100;
    private static final int NAME_H = 11;

    private @org.jetbrains.annotations.Nullable EditBox nameBox;
    private String lastSentName = "";

    public LinkerNodeScreen(LinkerNodeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        String initial = this.menu.customName();
        EditBox previous = this.nameBox;
        this.nameBox = new EditBox(this.font, this.leftPos + NAME_X, this.topPos + NAME_Y,
                NAME_W, NAME_H, Component.translatable("gui.frostyssortingdepot.linker_node.name"));
        this.nameBox.setMaxLength(LinkerNodeBlockEntity.MAX_NAME_LENGTH);
        this.nameBox.setBordered(true);
        this.nameBox.setHint(Component.translatable("gui.frostyssortingdepot.linker_node.name.hint"));
        this.nameBox.setValue(previous != null ? previous.getValue() : initial);
        this.lastSentName = initial;
        this.nameBox.setResponder(this::onNameChanged);
        addRenderableWidget(this.nameBox);
    }

    private void onNameChanged(String value) {
        if (value.equals(lastSentName)) {
            return;
        }
        lastSentName = value;
        // Optimistic local update so a reopen of this GUI in the same session shows the typed name
        // (BlockEntity NBT doesn't sync server→client outside of chunk loads).
        this.menu.getNode().setCustomName(value);
        // PacketDistributor.sendToServer doesn't exist on 1.21.x; send a ServerboundCustomPayloadPacket directly.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.getConnection().send(
                    new ServerboundCustomPayloadPacket(new RenameLinkerNodePayload(this.menu.nodePos(), value)));
        }
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameBox != null && this.nameBox.isFocused() && keyCode != InputConstants.KEY_ESCAPE) {
            this.nameBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
