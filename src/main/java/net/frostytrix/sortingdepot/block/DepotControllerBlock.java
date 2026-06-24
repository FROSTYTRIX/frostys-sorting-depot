package net.frostytrix.sortingdepot.block;

import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.frostytrix.sortingdepot.gui.DepotControllerMenu;
import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof DepotControllerBlockEntity controller
                ? controller.getComparatorSignal()
                : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DepotControllerBlockEntity controller) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new DepotControllerMenu(id, inv, controller),
                    Component.translatable("block.frostyssortingdepot.depot_controller")), pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof DepotControllerBlockEntity controller) {
            controller.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
