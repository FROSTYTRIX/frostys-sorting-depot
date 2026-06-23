package net.frostytrix.sortingdepot.registry;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.block.DepotControllerBlock;
import net.frostytrix.sortingdepot.block.LinkerNodeBlock;
import net.frostytrix.sortingdepot.block.OverflowChestBlock;
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

    /** The network hub: items inserted into its top face are routed to registered Linker Nodes. */
    public static final DeferredBlock<DepotControllerBlock> DEPOT_CONTROLLER =
            BLOCKS.registerBlock("depot_controller", DepotControllerBlock::new,
                    p -> p.mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    /** The network catch-all: receives anything no Linker Node accepted. */
    public static final DeferredBlock<OverflowChestBlock> OVERFLOW_CHEST =
            BLOCKS.registerBlock("overflow_chest", OverflowChestBlock::new,
                    p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
