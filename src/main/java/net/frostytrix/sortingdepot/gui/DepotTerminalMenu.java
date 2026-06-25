package net.frostytrix.sortingdepot.gui;

import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.frostytrix.sortingdepot.network.TerminalUpdatePayload;
import net.frostytrix.sortingdepot.registry.SDBlocks;
import net.frostytrix.sortingdepot.registry.SDMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the Depot Terminal: a slotless, read-only dashboard. The initial {@link TerminalSnapshot} is
 * captured server-side at open time and shipped in the open buffer; while the menu stays open the server
 * re-gathers and pushes a fresh snapshot every {@link #REFRESH_TICKS} ticks (via
 * {@link TerminalUpdatePayload}) so the dashboard updates live.
 */
public class DepotTerminalMenu extends AbstractContainerMenu {

    private static final int REFRESH_TICKS = 10; // ~2 refreshes/second

    private final ContainerLevelAccess access;
    private final BlockPos terminalPos;
    private final Player player;
    private TerminalSnapshot snapshot;
    private int sinceRefresh;

    /** Client-side: reads the terminal position and the initial network snapshot. */
    public DepotTerminalMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        super(SDMenus.DEPOT_TERMINAL.get(), containerId);
        this.player = playerInventory.player;
        this.terminalPos = buf.readBlockPos();
        this.access = ContainerLevelAccess.create(player.level(), terminalPos);
        this.snapshot = TerminalSnapshot.read(buf);
    }

    /** Server-side: starts empty; {@link #broadcastChanges()} pushes live snapshots to the viewer. */
    public DepotTerminalMenu(int containerId, Inventory playerInventory, BlockPos terminalPos) {
        super(SDMenus.DEPOT_TERMINAL.get(), containerId);
        this.player = playerInventory.player;
        this.terminalPos = terminalPos;
        this.access = ContainerLevelAccess.create(player.level(), terminalPos);
        this.snapshot = TerminalSnapshot.empty();
    }

    public TerminalSnapshot getSnapshot() {
        return snapshot;
    }

    /** Client: replace the displayed snapshot with a freshly-received one. */
    public void setSnapshot(TerminalSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player instanceof ServerPlayer serverPlayer && ++sinceRefresh >= REFRESH_TICKS) {
            sinceRefresh = 0;
            Level level = serverPlayer.level();
            DepotControllerBlockEntity controller = findController(level, terminalPos);
            TerminalSnapshot fresh = controller == null
                    ? TerminalSnapshot.empty()
                    : TerminalSnapshot.gather(level, controller);
            PacketDistributor.sendToPlayer(serverPlayer, new TerminalUpdatePayload(fresh));
        }
    }

    private static @Nullable DepotControllerBlockEntity findController(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof DepotControllerBlockEntity controller) {
                return controller;
            }
        }
        return null;
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, SDBlocks.DEPOT_TERMINAL.get());
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        return ItemStack.EMPTY; // no slots to move
    }
}
