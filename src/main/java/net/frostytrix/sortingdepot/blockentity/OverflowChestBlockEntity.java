package net.frostytrix.sortingdepot.blockentity;

import net.frostytrix.sortingdepot.block.OverflowChestBlock;
import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The network catch-all, now a real chest: a {@link ChestBlockEntity} so it gets the chest model, lid
 * animation, double-chest joining, and the vanilla chest GUI for free. The Controller routes into it via
 * its {@code Container} capability; vanilla handles the content drop on break.
 */
public class OverflowChestBlockEntity extends ChestBlockEntity {

    public OverflowChestBlockEntity(BlockPos pos, BlockState state) {
        super(SDBlockEntities.OVERFLOW_CHEST.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.frostyssortingdepot.overflow_chest");
    }

    /**
     * The full chest container — a {@code CompoundContainer} when this block is half of a double chest,
     * or just this single chest otherwise. Used by the routing/capability/snapshot layers so an Overflow
     * Chest's other half isn't silently ignored.
     *
     * <p>Returns {@code null} only when this BE has somehow been queried before placement (no level).
     */
    public Container fullContainer() {
        Level level = getLevel();
        if (level == null || !(getBlockState().getBlock() instanceof OverflowChestBlock block)) {
            return this;
        }
        // ChestBlock.getContainer returns a CompoundContainer when the chest is double, or the single
        // ChestBlockEntity otherwise. The `override` flag (true) bypasses the lid-occluded check, which
        // we don't care about for routing purposes.
        Container merged = ChestBlock.getContainer(block, getBlockState(), level, getBlockPos(), true);
        return merged == null ? this : merged;
    }
}
