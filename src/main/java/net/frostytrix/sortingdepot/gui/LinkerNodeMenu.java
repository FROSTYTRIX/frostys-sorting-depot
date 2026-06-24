package net.frostytrix.sortingdepot.gui;

import net.frostytrix.sortingdepot.blockentity.LinkerNodeBlockEntity;
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
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Menu for the Linker Node: a single Filter Card slot plus the player inventory.
 */
public class LinkerNodeMenu extends AbstractContainerMenu {

    private static final int CARD_SLOT = 0;
    private static final int INV_START = 1;

    private final LinkerNodeBlockEntity node;
    private final ContainerLevelAccess access;
    private boolean linked; // synced to client for the status display

    /** Client-side constructor: resolves the block entity from the synced position. */
    public LinkerNodeMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, resolve(playerInventory, data.readBlockPos()));
    }

    /** Server-side constructor. */
    public LinkerNodeMenu(int containerId, Inventory playerInventory, LinkerNodeBlockEntity node) {
        super(SDMenus.LINKER_NODE.get(), containerId);
        this.node = node;
        this.access = ContainerLevelAccess.create(node.getLevel(), node.getBlockPos());

        // Filter Card slot (the handler's isValid restricts it to Filter Cards).
        addSlot(new SlotItemHandler(node.getFilterSlot(), 0, 80, 47));

        // Sync the node's priority to the client so the screen can display it.
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return node.getPriority();
            }

            @Override
            public void set(int value) {
                node.setPriority(value);
            }
        });

        // Sync whether this node is registered to a Controller.
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return node.getControllerPos() != null ? 1 : 0;
            }

            @Override
            public void set(int value) {
                linked = value != 0;
            }
        });

        // Player inventory (3 rows) then hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static LinkerNodeBlockEntity resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        if (be instanceof LinkerNodeBlockEntity linkerNode) {
            return linkerNode;
        }
        throw new IllegalStateException("No Linker Node block entity at " + pos);
    }

    public LinkerNodeBlockEntity getNode() {
        return node;
    }

    /** Whether this node is registered to a Controller (synced to the client). */
    public boolean isLinked() {
        return linked;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, SDBlocks.LINKER_NODE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index == CARD_SLOT) {
                // Card slot -> player inventory.
                if (!moveItemStackTo(stack, INV_START, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory -> card slot (mayPlace enforces Filter-Card-only).
                if (!moveItemStackTo(stack, CARD_SLOT, INV_START, false)) {
                    return ItemStack.EMPTY;
                }
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
