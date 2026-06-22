package net.frostytrix.sortingdepot.registry;

import java.util.function.Supplier;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.blockentity.LinkerNodeBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All of the mod's block entity types.
 */
public final class SDBlockEntities {

    private SDBlockEntities() {
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FrostysSortingDepot.MOD_ID);

    public static final Supplier<BlockEntityType<LinkerNodeBlockEntity>> LINKER_NODE =
            BLOCK_ENTITIES.register("linker_node",
                    () -> new BlockEntityType<>(LinkerNodeBlockEntity::new, SDBlocks.LINKER_NODE.get()));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
