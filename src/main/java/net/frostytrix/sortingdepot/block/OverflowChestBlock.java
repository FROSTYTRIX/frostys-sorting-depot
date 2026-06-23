package net.frostytrix.sortingdepot.block;

import net.frostytrix.sortingdepot.blockentity.OverflowChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Overflow Chest block. A network catch-all placed adjacent to a Depot Controller.
 */
public class OverflowChestBlock extends Block implements EntityBlock {

    public OverflowChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OverflowChestBlockEntity(pos, state);
    }
}
