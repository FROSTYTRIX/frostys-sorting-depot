package net.frostytrix.sortingdepot.block;

import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.frostytrix.sortingdepot.gui.DepotTerminalMenu;
import net.frostytrix.sortingdepot.gui.TerminalSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The Depot Terminal: a read-only dashboard for an adjacent Controller's network. No block entity — it
 * resolves the Controller from its neighbours on use and snapshots the network into the menu buffer.
 */
public class DepotTerminalBlock extends Block {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DepotTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // The screen faces the player who placed it (like a wall monitor).
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            DepotControllerBlockEntity controller = findController(level, pos);
            if (controller == null) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("block.frostyssortingdepot.depot_terminal.no_controller"), true);
                }
                return InteractionResult.SUCCESS;
            }
            TerminalSnapshot snapshot = TerminalSnapshot.gather(level, controller);
            player.openMenu(
                    new SimpleMenuProvider(
                            (id, inv, p) -> new DepotTerminalMenu(id, inv, pos),
                            Component.translatable("block.frostyssortingdepot.depot_terminal")),
                    buf -> {
                        buf.writeBlockPos(pos);
                        snapshot.write(buf);
                    });
        }
        return InteractionResult.SUCCESS;
    }

    private static @Nullable DepotControllerBlockEntity findController(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof DepotControllerBlockEntity controller) {
                return controller;
            }
        }
        return null;
    }
}
