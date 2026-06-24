package net.frostytrix.sortingdepot.blockentity;

import net.frostytrix.sortingdepot.block.LinkerNodeBlock;
import net.frostytrix.sortingdepot.item.FilterCardItem;
import net.frostytrix.sortingdepot.item.PriorityStampItem;
import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.frostytrix.sortingdepot.routing.FilterMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the configuration for one sorting destination: a single Filter Card, a priority (1–5), and the
 * {@link BlockPos} of the Controller it is registered to. The served inventory is resolved by the
 * Controller from this node's facing direction (added in the routing phase).
 *
 * <p>Storage uses a classic {@link ItemStackHandler}; the slot is exposed to a {@code SlotItemHandler}
 * in the GUI.
 */
public class LinkerNodeBlockEntity extends BlockEntity {

    private static final String FILTER_SLOT_KEY = "filter_slot";

    private final ItemStackHandler filterSlot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof FilterCardItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int priority = PriorityStampItem.DEFAULT;
    @Nullable
    private BlockPos controllerPos;

    public LinkerNodeBlockEntity(BlockPos pos, BlockState state) {
        super(SDBlockEntities.LINKER_NODE.get(), pos, state);
    }

    /** The Filter Card slot, exposed for the menu/GUI. */
    public ItemStackHandler getFilterSlot() {
        return filterSlot;
    }

    /** The currently inserted Filter Card stack (may be empty). */
    public ItemStack getFilterCard() {
        return filterSlot.getStackInSlot(0);
    }

    /** The routing filter for this destination, or {@code null} if no (valid) card is inserted. */
    public @Nullable FilterMode getFilterMode() {
        ItemStack card = getFilterCard();
        if (card.getItem() instanceof FilterCardItem) {
            return FilterCardItem.data(card).toFilterMode();
        }
        return null;
    }

    /** The item capability of the inventory this node faces, or {@code null} if there is none. */
    public @Nullable IItemHandler getTargetHandler() {
        if (level == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(LinkerNodeBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);
        return level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, facing.getOpposite());
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = Math.clamp(priority, PriorityStampItem.MIN, PriorityStampItem.MAX);
        setChanged();
    }

    public @Nullable BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(@Nullable BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        setChanged();
    }

    /**
     * Drops the inserted Filter Card at {@code pos}. Called from the block's {@code onRemove} hook
     * because 1.21.1 has no {@code preRemoveSideEffects} on {@code BlockEntity}.
     */
    public void dropContents(net.minecraft.world.level.Level level, BlockPos pos) {
        ItemStack card = getFilterCard();
        if (!card.isEmpty()) {
            Block.popResource(level, pos, card);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag output, HolderLookup.Provider registries) {
        super.saveAdditional(output, registries);
        output.put(FILTER_SLOT_KEY, filterSlot.serializeNBT(registries));
        output.putInt("priority", priority);
        if (controllerPos != null) {
            output.put("controller", BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, controllerPos).getOrThrow());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag input, HolderLookup.Provider registries) {
        super.loadAdditional(input, registries);
        filterSlot.deserializeNBT(registries, input.getCompound(FILTER_SLOT_KEY));
        priority = input.contains("priority") ? input.getInt("priority") : PriorityStampItem.DEFAULT;
        Tag controllerTag = input.get("controller");
        controllerPos = controllerTag == null
                ? null
                : BlockPos.CODEC.parse(NbtOps.INSTANCE, controllerTag).result().orElse(null);
    }
}
