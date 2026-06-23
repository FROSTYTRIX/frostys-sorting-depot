package net.frostytrix.sortingdepot.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
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
    private final ItemStacksResourceHandler input = new ItemStacksResourceHandler(1) {
        @Override
        protected void onContentsChanged(int slot, ItemStack stack) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                // Keep comparators reading the buffer up to date.
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    };

    /** Registered Linker Nodes, in registration order (drives same-priority tie-breaking). */
    private final List<BlockPos> linkers = new ArrayList<>();

    public DepotControllerBlockEntity(BlockPos pos, BlockState state) {
        super(SDBlockEntities.DEPOT_CONTROLLER.get(), pos, state);
    }

    public ItemStacksResourceHandler getInputHandler() {
        return input;
    }

    /** Redstone signal (0–15) for a comparator: 0 when the buffer is empty, scaling up with how full it is. */
    public int getComparatorSignal() {
        ItemStack buffered = ItemUtil.getStack(input, 0);
        if (buffered.isEmpty()) {
            return 0;
        }
        float fill = (float) buffered.getCount() / Math.max(1, buffered.getMaxStackSize());
        return Mth.floor(fill * 14.0F) + 1;
    }

    public void addLinker(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (!linkers.contains(immutable)) {
            linkers.add(immutable);
            setChanged();
        }
    }

    public void removeLinker(BlockPos pos) {
        if (linkers.remove(pos.immutable())) {
            setChanged();
        }
    }

    public List<BlockPos> getLinkers() {
        return List.copyOf(linkers);
    }

    // --- ticking / routing ---------------------------------------------------------------------

    /** Ticks between transfers — a hopper is 8, so this is roughly four times a hopper's throughput. */
    private static final int TRANSFER_COOLDOWN = 2;

    private int transferCooldown;

    public static void serverTick(Level level, BlockPos pos, BlockState state, DepotControllerBlockEntity be) {
        if (be.transferCooldown > 0) {
            be.transferCooldown--;
            return;
        }
        // Hopper-style cadence: pull one item from above into the buffer, then route one item out.
        be.transferCooldown = TRANSFER_COOLDOWN;
        be.pullFromAbove(level);
        be.route(level, 1);
    }

    /** Pulls a single item from an inventory directly above into the input buffer (hopper-style). */
    private void pullFromAbove(Level level) {
        ResourceHandler<ItemResource> above =
                level.getCapability(Capabilities.Item.BLOCK, worldPosition.above(), Direction.DOWN);
        if (above == null) {
            return;
        }
        for (int slot = 0; slot < above.size(); slot++) {
            ItemResource resource = above.getResource(slot);
            if (resource.isEmpty()) {
                continue;
            }
            try (Transaction tx = Transaction.openRoot()) {
                int extracted = above.extract(slot, resource, 1, tx);
                if (extracted > 0 && input.insert(0, resource, extracted, tx) == extracted) {
                    tx.commit();
                    return;
                }
            }
        }
    }

    /** Routes up to {@code maxMove} items from the buffer to their destinations. Returns true if any moved. */
    private boolean route(Level level, int maxMove) {
        ItemStack remaining = ItemUtil.getStack(input, 0);
        if (remaining.isEmpty()) {
            return false;
        }
        int budget = maxMove;
        boolean changed = false;

        while (!remaining.isEmpty() && budget > 0) {
            RoutableItem routable = toRoutableItem(remaining);

            // Build aligned candidate/target lists from the registered nodes.
            List<RoutingEngine.Candidate> candidates = new ArrayList<>();
            List<ResourceHandler<ItemResource>> targets = new ArrayList<>();
            for (BlockPos linkerPos : linkers) {
                if (!(level.getBlockEntity(linkerPos) instanceof LinkerNodeBlockEntity node)) {
                    continue;
                }
                FilterMode mode = node.getFilterMode();
                if (mode == null) {
                    continue;
                }
                ResourceHandler<ItemResource> target = node.getTargetHandler();
                boolean room = target != null && hasRoom(target, remaining);
                candidates.add(new RoutingEngine.Candidate(mode, node.getPriority(), room));
                targets.add(target);
            }

            int index = RoutingEngine.chooseTarget(routable, candidates);
            ResourceHandler<ItemResource> target = index >= 0 ? targets.get(index) : findOverflow(level);
            if (target == null) {
                break; // nothing accepts it — leave it buffered, never voided
            }

            ItemStack toMove = remaining.copyWithCount(Math.min(budget, remaining.getCount()));
            int moved = toMove.getCount() - insert(target, toMove).getCount();
            if (moved == 0) {
                break; // everything full — avoid spinning
            }
            budget -= moved;
            remaining = remaining.copyWithCount(remaining.getCount() - moved);
            changed = true;
        }

        if (changed) {
            input.set(0, ItemResource.of(remaining), remaining.getCount());
            setChanged();
        }
        return changed;
    }

    /** The handler of an Overflow Chest adjacent to this Controller, or {@code null} if there is none. */
    private @Nullable ResourceHandler<ItemResource> findOverflow(Level level) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof OverflowChestBlockEntity overflow) {
                return overflow.getHandler();
            }
        }
        return null;
    }

    /** Builds the Minecraft-free routing snapshot, resolving the item's tags here at the adapter boundary. */
    private static RoutableItem toRoutableItem(ItemStack stack) {
        Set<String> tags = stack.typeHolder().tags()
                .map(tag -> tag.location().toString())
                .collect(Collectors.toSet());
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return new RoutableItem(itemId, stack.getCount(), tags, stack.getComponentsPatch().toString());
    }

    /** Simulated check: would any of {@code stack} fit into {@code target}? */
    private static boolean hasRoom(ResourceHandler<ItemResource> target, ItemStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            ItemStack remaining = ItemUtil.insertItemReturnRemaining(target, stack, true, tx);
            return remaining.getCount() < stack.getCount();
        }
    }

    /** Inserts as much of {@code stack} as fits into {@code target}, committing it. Returns the remainder. */
    private static ItemStack insert(ResourceHandler<ItemResource> target, ItemStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            ItemStack remaining = ItemUtil.insertItemReturnRemaining(target, stack, false, tx);
            tx.commit();
            return remaining;
        }
    }

    // --- persistence ---------------------------------------------------------------------------

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ItemStack buffered = ItemUtil.getStack(input, 0);
        if (level != null && !buffered.isEmpty()) {
            Block.popResource(level, pos, buffered);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        input.serialize(output.child(INPUT_KEY));
        output.store("linkers", BlockPos.CODEC.listOf(), linkers);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child(INPUT_KEY).ifPresent(this.input::deserialize);
        linkers.clear();
        linkers.addAll(input.read("linkers", BlockPos.CODEC.listOf()).orElse(List.of()));
    }
}
