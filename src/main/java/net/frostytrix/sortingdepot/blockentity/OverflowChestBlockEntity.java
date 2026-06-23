package net.frostytrix.sortingdepot.blockentity;

import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;

/**
 * The network's catch-all. A 27-slot inventory the Controller routes to when no Linker Node accepts an
 * item (or every match is full). Exposed as the item capability on all faces for pipe/hopper extraction.
 */
public class OverflowChestBlockEntity extends BlockEntity {

    private static final String ITEMS_KEY = "items";

    private final ItemStacksResourceHandler items = new ItemStacksResourceHandler(27) {
        @Override
        protected void onContentsChanged(int slot, ItemStack stack) {
            setChanged();
        }
    };

    public OverflowChestBlockEntity(BlockPos pos, BlockState state) {
        super(SDBlockEntities.OVERFLOW_CHEST.get(), pos, state);
    }

    public ItemStacksResourceHandler getHandler() {
        return items;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = ItemUtil.getStack(items, slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        items.serialize(output.child(ITEMS_KEY));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child(ITEMS_KEY).ifPresent(items::deserialize);
    }
}
