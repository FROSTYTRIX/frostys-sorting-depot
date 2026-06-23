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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the configuration for one sorting destination: a single Filter Card, a priority (1–5), and the
 * {@link BlockPos} of the Controller it is registered to. The served inventory is resolved by the
 * Controller from this node's facing direction (added in the routing phase).
 *
 * <p>Storage uses the 26.2 transfer API ({@link ItemStacksResourceHandler}), so the slot can be exposed
 * directly to a {@code ResourceHandlerSlot} in the GUI.
 */
public class LinkerNodeBlockEntity extends BlockEntity {

    private static final String FILTER_SLOT_KEY = "filter_slot";

    private final ItemStacksResourceHandler filterSlot = new ItemStacksResourceHandler(1) {
        @Override
        public boolean isValid(int slot, ItemResource resource) {
            return resource.getItem() instanceof FilterCardItem;
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack stack) {
            setChanged();
        }
    };

    private int priority = PriorityStampItem.DEFAULT;
    @Nullable
    private BlockPos controllerPos;

    public LinkerNodeBlockEntity(BlockPos pos, BlockState state) {
        super(SDBlockEntities.LINKER_NODE.get(), pos, state);
    }

    /** The Filter Card slot, exposed for the menu/GUI and as a capability source. */
    public ItemStacksResourceHandler getFilterSlot() {
        return filterSlot;
    }

    /** The currently inserted Filter Card stack (may be empty). */
    public ItemStack getFilterCard() {
        return ItemUtil.getStack(filterSlot, 0);
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
    public @Nullable ResourceHandler<ItemResource> getTargetHandler() {
        if (level == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(LinkerNodeBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);
        return level.getCapability(Capabilities.Item.BLOCK, targetPos, facing.getOpposite());
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

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        ItemStack card = getFilterCard();
        if (level != null && !card.isEmpty()) {
            Block.popResource(level, pos, card);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        filterSlot.serialize(output.child(FILTER_SLOT_KEY));
        output.putInt("priority", priority);
        output.storeNullable("controller", BlockPos.CODEC, controllerPos);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child(FILTER_SLOT_KEY).ifPresent(filterSlot::deserialize);
        priority = input.getIntOr("priority", PriorityStampItem.DEFAULT);
        controllerPos = input.read("controller", BlockPos.CODEC).orElse(null);
    }
}
