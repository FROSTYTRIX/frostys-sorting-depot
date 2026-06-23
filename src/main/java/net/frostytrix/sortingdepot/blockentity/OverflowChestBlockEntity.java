package net.frostytrix.sortingdepot.blockentity;

import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
}
