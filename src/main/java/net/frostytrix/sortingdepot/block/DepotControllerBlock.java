package net.frostytrix.sortingdepot.block;

import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Depot Controller block. Hosts the {@link DepotControllerBlockEntity} and drives its server tick.
 */
public class DepotControllerBlock extends Block implements EntityBlock {

    public DepotControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DepotControllerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Routing is server-only.
        if (level.isClientSide() || type != SDBlockEntities.DEPOT_CONTROLLER.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> DepotControllerBlockEntity.serverTick(lvl, pos, st, (DepotControllerBlockEntity) be);
    }
}
