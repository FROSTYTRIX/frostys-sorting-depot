package net.frostytrix.sortingdepot.block;

import net.frostytrix.sortingdepot.blockentity.OverflowChestBlockEntity;
import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Overflow Chest block. Extends {@link ChestBlock} so it behaves exactly like a vanilla chest —
 * facing, double-chest joining, waterlogging, the opening animation, and the chest GUI — while still
 * being our distinct block (its block entity is registered as the network's overflow target).
 */
public class OverflowChestBlock extends ChestBlock {

    public OverflowChestBlock(Properties properties) {
        super(SDBlockEntities.OVERFLOW_CHEST::get, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OverflowChestBlockEntity(pos, state);
    }
}
