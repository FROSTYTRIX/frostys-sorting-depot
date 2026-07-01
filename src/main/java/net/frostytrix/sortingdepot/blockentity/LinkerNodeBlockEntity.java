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
    /** Optional player-supplied name shown in the Depot Terminal; empty means "use the raw block name". */
    private String customName = "";
    /** When false, routing skips this destination entirely (a soft off-switch that keeps the card/priority). */
    private boolean enabled = true;
    /**
     * Which face of the target inventory items are inserted into. {@code null} = automatic (the face
     * this node points at, i.e. {@code facing.getOpposite()}), matching vanilla hopper behaviour. A set
     * value lets one node feed a specific side — e.g. the top of a furnace for smeltables vs the side for fuel.
     */
    @Nullable
    private Direction insertSide;

    /** Hard cap on a node's custom name (in characters). Kept conservative for Terminal row width. */
    public static final int MAX_NAME_LENGTH = 32;

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

    /**
     * The item capability of the inventory this node faces, or {@code null} if there is none. If the
     * server-side {@code validDestinationTag} whitelist is set, the target block must be in that tag
     * — otherwise this returns {@code null} and routing skips this destination. Lets pack admins keep
     * items out of hoppers/pipes by accident.
     */
    public @Nullable ResourceHandler<ItemResource> getTargetHandler() {
        if (level == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(LinkerNodeBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);
        var tag = net.frostytrix.sortingdepot.CommonConfig.validDestinationTag();
        if (tag.isPresent() && !level.getBlockState(targetPos).is(tag.get())) {
            return null;
        }
        // Insert from the node's facing side by default (hopper-like); an override picks a specific face.
        Direction side = insertSide != null ? insertSide : facing.getOpposite();
        return level.getCapability(Capabilities.Item.BLOCK, targetPos, side);
    }

    /**
     * Vanilla-style comparator signal scaled to how full the target inventory is: 0 when empty, 15 when
     * every slot is full of max-stack items. Mirrors
     * {@link net.minecraft.world.inventory.AbstractContainerMenu#getRedstoneSignalFromContainer} but works
     * on the transfer-API {@link ResourceHandler} this node already resolves. Returns 0 if the target has
     * no item capability (or the {@code validDestinationTag} whitelist rejects it).
     */
    public int getComparatorSignal() {
        ResourceHandler<ItemResource> target = getTargetHandler();
        if (target == null || target.size() == 0) {
            return 0;
        }
        float fillSum = 0.0F;
        int filledSlots = 0;
        for (int slot = 0; slot < target.size(); slot++) {
            ItemStack stack = ItemUtil.getStack(target, slot);
            if (!stack.isEmpty()) {
                filledSlots++;
                fillSum += (float) stack.getCount() / stack.getMaxStackSize();
            }
        }
        if (filledSlots == 0) {
            return 0;
        }
        return (int) (fillSum / target.size() * 14.0F) + 1;
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

    /** The player-supplied name, or {@code ""} if unset. Use {@link #displayName(String)} to render. */
    public String getCustomName() {
        return customName;
    }

    /** Trims, clamps to {@link #MAX_NAME_LENGTH}, and stores. Empty / whitespace clears it. */
    public void setCustomName(String name) {
        String trimmed = name == null ? "" : name.strip();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_NAME_LENGTH);
        }
        if (!trimmed.equals(customName)) {
            customName = trimmed;
            setChanged();
        }
    }

    /** Helper for callers that have a fallback ready: returns the custom name when set, else {@code fallback}. */
    public String displayName(String fallback) {
        return customName.isEmpty() ? fallback : customName;
    }

    /** Whether routing may send items here. Disabled nodes keep their card + priority but are skipped. */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            setChanged();
        }
    }

    /** The insert-side override, or {@code null} for automatic (the node's facing side). */
    public @Nullable Direction getInsertSide() {
        return insertSide;
    }

    /** Client-side {@code DataSlot} sync setter: 0 = automatic, else {@code Direction.ordinal() + 1}. */
    public void setInsertSideFromSync(int value) {
        insertSide = value <= 0 ? null : Direction.values()[value - 1];
    }

    /** Cycles the insert side: automatic → DOWN → UP → NORTH → SOUTH → WEST → EAST → automatic. */
    public void cycleInsertSide() {
        if (insertSide == null) {
            insertSide = Direction.values()[0];
        } else if (insertSide.ordinal() + 1 >= Direction.values().length) {
            insertSide = null;
        } else {
            insertSide = Direction.values()[insertSide.ordinal() + 1];
        }
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
        if (!customName.isEmpty()) {
            output.putString("custom_name", customName);
        }
        if (!enabled) {
            output.putBoolean("enabled", false);
        }
        output.storeNullable("insert_side", Direction.CODEC, insertSide);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child(FILTER_SLOT_KEY).ifPresent(filterSlot::deserialize);
        priority = input.getIntOr("priority", PriorityStampItem.DEFAULT);
        controllerPos = input.read("controller", BlockPos.CODEC).orElse(null);
        customName = input.getStringOr("custom_name", "");
        enabled = input.getBooleanOr("enabled", true);
        insertSide = input.read("insert_side", Direction.CODEC).orElse(null);
    }
}
