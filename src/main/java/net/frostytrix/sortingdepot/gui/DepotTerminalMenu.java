package net.frostytrix.sortingdepot.gui;

import net.frostytrix.sortingdepot.registry.SDBlocks;
import net.frostytrix.sortingdepot.registry.SDMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Depot Terminal: a slotless, read-only dashboard. The network {@link TerminalSnapshot} is
 * captured server-side at open time and shipped to the client in the open buffer.
 */
public class DepotTerminalMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final TerminalSnapshot snapshot;

    /** Client-side: reads the terminal position and the network snapshot. */
    public DepotTerminalMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(SDMenus.DEPOT_TERMINAL.get(), containerId);
        BlockPos terminalPos = buf.readBlockPos();
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), terminalPos);
        this.snapshot = TerminalSnapshot.read(buf);
    }

    /** Server-side: no display data needed (the dashboard is client-only). */
    public DepotTerminalMenu(int containerId, Inventory playerInventory, BlockPos terminalPos) {
        super(SDMenus.DEPOT_TERMINAL.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), terminalPos);
        this.snapshot = TerminalSnapshot.empty();
    }

    public TerminalSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, SDBlocks.DEPOT_TERMINAL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // no slots to move
    }
}
