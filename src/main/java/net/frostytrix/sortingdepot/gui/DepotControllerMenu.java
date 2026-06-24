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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Menu for the Depot Controller: exposes the single input-buffer slot (so players can see and retrieve
 * items waiting to be routed) plus the player inventory.
 */
public class DepotControllerMenu extends AbstractContainerMenu {

    private static final int INPUT_SLOT = 0;
    private static final int INV_START = 1;

    private final ContainerLevelAccess access;

    public DepotControllerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, resolve(playerInventory, data.readBlockPos()));
    }

    public DepotControllerMenu(int containerId, Inventory playerInventory, DepotControllerBlockEntity controller) {
        super(SDMenus.DEPOT_CONTROLLER.get(), containerId);
        this.access = ContainerLevelAccess.create(controller.getLevel(), controller.getBlockPos());

        // Compact layout: single input slot up top, player inventory directly below (no chest-sized gap).
        addSlot(new SlotItemHandler(controller.getInputHandler(), 0, 80, 18));

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
