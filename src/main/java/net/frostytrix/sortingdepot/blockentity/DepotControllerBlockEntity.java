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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

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

    public static void serverTick(Level level, BlockPos pos, BlockState state, DepotControllerBlockEntity be) {
        // Idle controllers do nothing (no energy, no work when the buffer is empty).
        if (ItemUtil.getStack(be.input, 0).isEmpty()) {
            return;
        }
        be.route(level);
    }

    private void route(Level level) {
        ItemStack remaining = ItemUtil.getStack(input, 0);
        boolean changed = false;

        while (!remaining.isEmpty()) {
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
            if (index < 0) {
                break; // no match or all full — leave the rest in the buffer (Overflow Chest comes later)
            }

            int before = remaining.getCount();
            remaining = insert(targets.get(index), remaining);
            if (remaining.getCount() == before) {
                break; // safety: made no progress, avoid spinning
            }
            changed = true;
        }

        if (changed) {
            input.set(0, ItemResource.of(remaining), remaining.getCount());
            setChanged();
        }
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
