package net.frostytrix.sortingdepot.block;

import net.frostytrix.sortingdepot.blockentity.LinkerNodeBlockEntity;
import net.frostytrix.sortingdepot.gui.LinkerNodeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Linker Node block. Placed facing a target inventory; its block entity holds the Filter Card and
 * priority for that destination. {@code facing} points toward the inventory it serves.
 */
public class LinkerNodeBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public LinkerNodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // Target the block this node is placed against (intuitive: click the chest to feed it). If placed
        // on a floor/ceiling, fall back to facing away from the player.
        Direction againstTarget = context.getClickedFace().getOpposite();
        Direction facing = againstTarget.getAxis().isHorizontal()
                ? againstTarget
                : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LinkerNodeBlockEntity(pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof LinkerNodeBlockEntity node
                ? node.getComparatorSignal()
                : 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LinkerNodeBlockEntity node) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new LinkerNodeMenu(id, inv, node),
                    Component.translatable("block.frostyssortingdepot.linker_node")), pos);
        }
        return InteractionResult.SUCCESS;
    }
}
