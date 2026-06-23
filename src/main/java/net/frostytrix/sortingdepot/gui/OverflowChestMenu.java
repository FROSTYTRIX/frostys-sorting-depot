package net.frostytrix.sortingdepot.gui;

import net.frostytrix.sortingdepot.blockentity.OverflowChestBlockEntity;
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
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/**
 * Menu for the Overflow Chest: a 27-slot (3×9) inventory plus the player inventory, laid out like a
 * vanilla single chest so it can reuse the {@code generic_54} background.
 */
public class OverflowChestMenu extends AbstractContainerMenu {

    private static final int ROWS = 3;
    private static final int COLS = 9;
    private static final int CONTAINER_SLOTS = ROWS * COLS;

    private final ContainerLevelAccess access;

    public OverflowChestMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, resolve(playerInventory, data.readBlockPos()));
    }

    public OverflowChestMenu(int containerId, Inventory playerInventory, OverflowChestBlockEntity be) {
        super(SDMenus.OVERFLOW_CHEST.get(), containerId);
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        ItemStacksResourceHandler handler = be.getHandler();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = col + row * COLS;
                addSlot(new ResourceHandlerSlot(handler, handler::set, index, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory, positioned exactly as a vanilla 3-row chest.
        int offset = (ROWS - 4) * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + offset));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + offset));
        }
    }

    private static OverflowChestBlockEntity resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (be instanceof OverflowChestBlockEntity overflow) {
            return overflow;
        }
        throw new IllegalStateException("No Overflow Chest block entity at " + pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, SDBlocks.OVERFLOW_CHEST.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index < CONTAINER_SLOTS) {
                if (!moveItemStackTo(stack, CONTAINER_SLOTS, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, CONTAINER_SLOTS, false)) {
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
