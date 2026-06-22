package net.frostytrix.sortingdepot.registry;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.block.LinkerNodeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All of the mod's blocks. Block items are registered alongside their items in {@link SDItems}.
 */
public final class SDBlocks {

    private SDBlocks() {
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FrostysSortingDepot.MOD_ID);

    /** A placed destination marker: holds a Filter Card + priority and faces a target inventory. */
    public static final DeferredBlock<LinkerNodeBlock> LINKER_NODE =
            BLOCKS.registerBlock("linker_node", LinkerNodeBlock::new,
                    p -> p.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
