package net.frostytrix.sortingdepot.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.blockentity.LinkerNodeBlockEntity;
import net.frostytrix.sortingdepot.network.RenameLinkerNodePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;
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

    // Name field (relative to leftPos/topPos).
    private static final int NAME_X = 8;
    private static final int NAME_Y = 6;
    private static final int NAME_W = 100;
    private static final int NAME_H = 11;

    private @org.jetbrains.annotations.Nullable EditBox nameBox;
    /** The last value we transmitted; used to skip duplicate packets while the responder fires. */
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
        // PacketDistributor.sendToServer doesn't exist on 26.2; send a ServerboundCustomPayloadPacket directly.
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.getConnection().send(
                    new ServerboundCustomPayloadPacket(new RenameLinkerNodePayload(this.menu.nodePos(), value)));
        }
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
        // Skip the default title (the name field takes that horizontal slot).
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

    @Override
    public boolean keyPressed(KeyEvent event) {
        // While the name field has focus, swallow non-Escape keys so the inventory keybind ("E" by
        // default) doesn't close the menu mid-rename.
        if (this.nameBox != null && this.nameBox.isFocused() && event.key() != InputConstants.KEY_ESCAPE) {
            this.nameBox.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }
}
