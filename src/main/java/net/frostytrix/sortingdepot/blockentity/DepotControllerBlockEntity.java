package net.frostytrix.sortingdepot.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.frostytrix.sortingdepot.CommonConfig;
import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.frostytrix.sortingdepot.routing.FilterMode;
import net.frostytrix.sortingdepot.routing.RoutableItem;
import net.frostytrix.sortingdepot.routing.RoutingEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * The heart of a sorting network. A single-slot input buffer (exposed as the item capability on the top
 * face) is drained each server tick: one destination is chosen by the pure {@link RoutingEngine} from the
 * registered Linker Nodes, and the item is pushed into that node's target inventory via the transfer API.
 *
 * <p>If nothing matches (or every match is full) the item simply stays in the input buffer — never voided.
 * The Overflow Chest fallback is added in a later phase.
 */
public class DepotControllerBlockEntity extends BlockEntity {

    private static final String INPUT_KEY = "input";

    /** Single-slot input buffer; hoppers/pipes insert here, exposed as the capability on top. */
    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                // Keep comparators reading the buffer up to date.
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    };

    /** Registered Linker Nodes, in registration order (drives same-priority tie-breaking). */
    private final List<BlockPos> linkers = new ArrayList<>();

    /** When true, equal-priority destinations are filled in rotation instead of first-to-last. */
    private boolean roundRobin;
    /** Rotation counter for round-robin; advanced after each successful move to a real destination. */
    private int rrCursor;

    public DepotControllerBlockEntity(BlockPos pos, BlockState state) {
        super(SDBlockEntities.DEPOT_CONTROLLER.get(), pos, state);
    }

    public ItemStackHandler getInputHandler() {
        return input;
    }

    public boolean isRoundRobin() {
        return roundRobin;
    }

    public void setRoundRobin(boolean value) {
        this.roundRobin = value;
        setChanged();
    }

    /** Redstone signal (0–15) for a comparator: 0 when the buffer is empty, scaling up with how full it is. */
    public int getComparatorSignal() {
        ItemStack buffered = input.getStackInSlot(0);
        if (buffered.isEmpty()) {
            return 0;
        }
        float fill = (float) buffered.getCount() / Math.max(1, buffered.getMaxStackSize());
        return Mth.floor(fill * 14.0F) + 1;
    }

    /**
     * Registers a new Linker Node. Refuses (returns {@code false}) when the network is already at the
     * configured cap so a misconfigured pack can't grow unbounded networks.
     */
    public boolean addLinker(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (linkers.contains(immutable)) {
            return true; // already registered, treat as success
        }
        if (linkers.size() >= CommonConfig.MAX_NETWORK_SIZE.get()) {
            return false;
        }
        linkers.add(immutable);
        setChanged();
        syncToClient();
        return true;
    }

    public void removeLinker(BlockPos pos) {
        if (linkers.remove(pos.immutable())) {
            setChanged();
            syncToClient();
        }
    }

    /** Pushes a block update so the client copy of {@link #linkers} stays current (used by the beam). */
    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public List<BlockPos> getLinkers() {
        return List.copyOf(linkers);
    }

    // --- ticking / routing ---------------------------------------------------------------------

    private int transferCooldown;

    public static void serverTick(Level level, BlockPos pos, BlockState state, DepotControllerBlockEntity be) {
        if (be.transferCooldown > 0) {
            be.transferCooldown--;
            return;
        }
        // Hopper-style cadence: pull one item from above into the buffer, then route a configurable
        // number of items out. Both knobs come from CommonConfig so server admins can tune throughput.
        be.transferCooldown = CommonConfig.TRANSFER_COOLDOWN.get();
        be.pullFromAbove(level);
        be.route(level, CommonConfig.BATCH_SIZE.get());
    }

    /** Pulls a single item from an inventory directly above into the input buffer (hopper-style). */
    private void pullFromAbove(Level level) {
        IItemHandler above =
                level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.above(), Direction.DOWN);
        if (above == null) {
            return;
        }
        for (int slot = 0; slot < above.getSlots(); slot++) {
            ItemStack peek = above.extractItem(slot, 1, true);
            if (peek.isEmpty()) {
                continue;
            }
            // Only pull if the buffer can take it, then commit both sides.
            if (input.insertItem(0, peek, true).isEmpty()) {
                ItemStack taken = above.extractItem(slot, 1, false);
                input.insertItem(0, taken, false);
                return;
            }
        }
    }

    /** Routes up to {@code maxMove} items from the buffer to their destinations. Returns true if any moved. */
    private boolean route(Level level, int maxMove) {
        ItemStack remaining = input.getStackInSlot(0);
        if (remaining.isEmpty()) {
            return false;
        }
        int budget = maxMove;
        boolean changed = false;

        while (!remaining.isEmpty() && budget > 0) {
            RoutableItem routable = toRoutableItem(remaining);

            // Build aligned candidate/target lists from the registered nodes.
            List<RoutingEngine.Candidate> candidates = new ArrayList<>();
            List<IItemHandler> targets = new ArrayList<>();
            for (BlockPos linkerPos : linkers) {
                if (!(level.getBlockEntity(linkerPos) instanceof LinkerNodeBlockEntity node)) {
                    continue;
                }
                FilterMode mode = node.getFilterMode();
                if (mode == null) {
                    continue;
                }
                IItemHandler target = node.getTargetHandler();
                boolean room = target != null && hasRoom(target, remaining);
                candidates.add(new RoutingEngine.Candidate(mode, node.getPriority(), room));
                targets.add(target);
            }

            int index = roundRobin
                    ? RoutingEngine.chooseRoundRobin(routable, candidates, rrCursor)
                    : RoutingEngine.chooseTarget(routable, candidates);
            IItemHandler target = index >= 0 ? targets.get(index) : findOverflow(level);
            if (target == null) {
                break; // nothing accepts it — leave it buffered, never voided
            }

            ItemStack toMove = remaining.copyWithCount(Math.min(budget, remaining.getCount()));
            int moved = toMove.getCount() - insert(target, toMove).getCount();
            if (moved == 0) {
                break; // everything full — avoid spinning
            }
            // Advance the rotation only after a real (non-overflow) destination accepted something.
            if (roundRobin && index >= 0) {
                rrCursor++;
            }
            budget -= moved;
            remaining = remaining.copyWithCount(remaining.getCount() - moved);
            changed = true;
        }

        if (changed) {
            input.setStackInSlot(0, remaining);
            setChanged();
        }
        return changed;
    }

    /** The inventory of an Overflow Chest adjacent to this Controller, or {@code null} if there is none. */
    private @Nullable IItemHandler findOverflow(Level level) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof OverflowChestBlockEntity overflow) {
                return new InvWrapper(overflow);
            }
        }
        return null;
    }

    /** Builds the Minecraft-free routing snapshot, resolving the item's tags here at the adapter boundary. */
    private static RoutableItem toRoutableItem(ItemStack stack) {
        Set<String> tags = stack.getTags()
                .map(tag -> tag.location().toString())
                .collect(Collectors.toSet());
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return new RoutableItem(itemId, stack.getCount(), tags, stack.getComponentsPatch().toString());
    }

    /** Simulated check: would any of {@code stack} fit into {@code target}? */
    private static boolean hasRoom(IItemHandler target, ItemStack stack) {
        ItemStack remaining = ItemHandlerHelper.insertItem(target, stack, true);
        return remaining.getCount() < stack.getCount();
    }

    /** Inserts as much of {@code stack} as fits into {@code target}. Returns the remainder. */
    private static ItemStack insert(IItemHandler target, ItemStack stack) {
        return ItemHandlerHelper.insertItem(target, stack, false);
    }

    // --- client sync (for the Linker wiring beam) ----------------------------------------------

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveCustomOnly(registries); // includes the linker list
    }

    @Override
    public @Nullable net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // --- persistence ---------------------------------------------------------------------------

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ItemStack buffered = input.getStackInSlot(0);
        if (level != null && !buffered.isEmpty()) {
            Block.popResource(level, pos, buffered);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        input.serialize(output.child(INPUT_KEY));
        output.store("linkers", BlockPos.CODEC.listOf(), linkers);
        output.putBoolean("round_robin", roundRobin);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child(INPUT_KEY).ifPresent(this.input::deserialize);
        linkers.clear();
        linkers.addAll(input.read("linkers", BlockPos.CODEC.listOf()).orElse(List.of()));
        roundRobin = input.getBooleanOr("round_robin", false);
    }
}
