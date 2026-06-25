package net.frostytrix.sortingdepot.gui;

import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.frostytrix.sortingdepot.registry.SDBlocks;
import net.frostytrix.sortingdepot.registry.SDMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Menu for the Depot Controller: exposes the single input-buffer slot (so players can see and retrieve
 * items waiting to be routed) plus the player inventory.
 */
public class DepotControllerMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int INV_START = 1;

    /** Button id (via clickMenuButton) that toggles round-robin distribution. */
    public static final int BTN_TOGGLE_ROUND_ROBIN = 0;

    private final ContainerLevelAccess access;
    private final DepotControllerBlockEntity controller;
    private boolean roundRobin; // synced to the client for the toggle button's state

    public DepotControllerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, resolve(playerInventory, data.readBlockPos()));
    }

    public DepotControllerMenu(int containerId, Inventory playerInventory, DepotControllerBlockEntity controller) {
        super(SDMenus.DEPOT_CONTROLLER.get(), containerId);
        this.controller = controller;
        this.access = ContainerLevelAccess.create(controller.getLevel(), controller.getBlockPos());

        // Compact layout: single input slot up top, player inventory directly below (no chest-sized gap).
        addSlot(new ResourceHandlerSlot(controller.getInputHandler(), controller.getInputHandler()::set, 0, 80, 18));

        // Sync round-robin state so the screen can label its toggle button.
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return controller.isRoundRobin() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                roundRobin = value != 0;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 109));
        }
    }

    private static DepotControllerBlockEntity resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (be instanceof DepotControllerBlockEntity controller) {
            return controller;
        }
        throw new IllegalStateException("No Depot Controller block entity at " + pos);
    }

    /** Whether round-robin is on (synced to the client for the button label). */
    public boolean isRoundRobin() {
        return roundRobin;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BTN_TOGGLE_ROUND_ROBIN) {
            controller.setRoundRobin(!controller.isRoundRobin());
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, SDBlocks.DEPOT_CONTROLLER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index == INPUT_SLOT) {
                if (!moveItemStackTo(stack, INV_START, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, INPUT_SLOT, INV_START, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return moved;
    }
}
