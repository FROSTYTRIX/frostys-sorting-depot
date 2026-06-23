package net.frostytrix.sortingdepot.block;

import net.frostytrix.sortingdepot.blockentity.OverflowChestBlockEntity;
import net.frostytrix.sortingdepot.gui.OverflowChestMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof OverflowChestBlockEntity be) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new OverflowChestMenu(id, inv, be),
                    Component.translatable("block.frostyssortingdepot.overflow_chest")), pos);
        }
        return InteractionResult.SUCCESS;
    }
}
