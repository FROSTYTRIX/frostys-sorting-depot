package net.frostytrix.sortingdepot.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.frostytrix.sortingdepot.block.LinkerNodeBlock;
import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.frostytrix.sortingdepot.blockentity.LinkerNodeBlockEntity;
import net.frostytrix.sortingdepot.blockentity.OverflowChestBlockEntity;
import net.frostytrix.sortingdepot.item.FilterCardItem;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import org.jetbrains.annotations.Nullable;

/**
 * A read-only snapshot of a Controller's network, captured server-side when a Depot Terminal is opened
 * and sent to the client for display. Static (reopen the Terminal to refresh).
 *
 * @param linkers     one row per registered Linker Node
 * @param hasOverflow whether an Overflow Chest is attached
 * @param overflowFill overflow fill, as a percent of slots used
 * @param inputCount  items currently sitting in the Controller's input buffer
 */
public record TerminalSnapshot(List<Entry> linkers, boolean hasOverflow, int overflowFill, int inputCount) {

    /**
     * One registered destination: the inventory it serves, its filter, how full it is (slot %), and the
     * Linker Node's own {@link BlockPos} (used by the Terminal's click-to-highlight feature to point the
     * player at the right node in-world).
     */
    public record Entry(BlockPos pos, String target, String filter, int fill) {
    }

    public static TerminalSnapshot empty() {
        return new TerminalSnapshot(List.of(), false, 0, 0);
    }

    // --- gathering (server) --------------------------------------------------------------------

    public static TerminalSnapshot gather(Level level, DepotControllerBlockEntity controller) {
        List<Entry> entries = new ArrayList<>();
        for (BlockPos pos : controller.getLinkers()) {
            if (level.getBlockEntity(pos) instanceof LinkerNodeBlockEntity node) {
                entries.add(new Entry(pos, targetName(level, node), describeFilter(node.getFilterCard()),
                        fillPercent(node.getTargetHandler())));
            }
        }
        ResourceHandler<ItemResource> overflow = adjacentOverflow(level, controller.getBlockPos());
        int inputCount = ItemUtil.getStack(controller.getInputHandler(), 0).getCount();
        return new TerminalSnapshot(entries, overflow != null, fillPercent(overflow), inputCount);
    }

    private static String targetName(Level level, LinkerNodeBlockEntity node) {
        if (node.getTargetHandler() == null) {
            return "(no inventory)";
        }
        Direction facing = node.getBlockState().getValue(LinkerNodeBlock.FACING);
        BlockPos targetPos = node.getBlockPos().relative(facing);
        return level.getBlockState(targetPos).getBlock().getName().getString();
    }

    private static String describeFilter(ItemStack card) {
        if (!(card.getItem() instanceof FilterCardItem)) {
            return "(no card)";
        }
        FilterCardData data = FilterCardItem.data(card);
        return switch (data.mode()) {
            case ITEM -> {
                String prefix = data.strict() ? "Items (NBT): " : "Items: ";
                yield data.items().isEmpty()
                        ? prefix + "-"
                        : prefix + data.items().stream()
                                .map(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getPath())
                                .collect(Collectors.joining(", "));
            }
            case MOD -> data.items().isEmpty()
                    ? "Mods: -"
                    : "Mods: " + data.items().stream()
                            .map(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getNamespace())
                            .distinct().collect(Collectors.joining(", "));
            case TAG -> data.tags().isEmpty()
                    ? "Tags: -"
                    : data.tags().stream().map(id -> "#" + id.getPath()).collect(Collectors.joining(", "));
            case OVERFLOW -> "Overflow";
        };
    }

    private static int fillPercent(@Nullable ResourceHandler<ItemResource> handler) {
        if (handler == null || handler.size() == 0) {
            return 0;
        }
        int used = 0;
        for (int slot = 0; slot < handler.size(); slot++) {
            if (!ItemUtil.getStack(handler, slot).isEmpty()) {
                used++;
            }
        }
        return used * 100 / handler.size();
    }

    private static @Nullable ResourceHandler<ItemResource> adjacentOverflow(Level level, BlockPos controllerPos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(controllerPos.relative(direction)) instanceof OverflowChestBlockEntity overflow) {
                return VanillaContainerWrapper.of(overflow.fullContainer());
            }
        }
        return null;
    }

    // --- network (buffer) ----------------------------------------------------------------------

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(linkers.size());
        for (Entry entry : linkers) {
            buf.writeBlockPos(entry.pos());
            buf.writeUtf(entry.target());
            buf.writeUtf(entry.filter());
            buf.writeVarInt(entry.fill());
        }
        buf.writeBoolean(hasOverflow);
        buf.writeVarInt(overflowFill);
        buf.writeVarInt(inputCount);
    }

    public static TerminalSnapshot read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readBlockPos(), buf.readUtf(), buf.readUtf(), buf.readVarInt()));
        }
        return new TerminalSnapshot(entries, buf.readBoolean(), buf.readVarInt(), buf.readVarInt());
    }
}
