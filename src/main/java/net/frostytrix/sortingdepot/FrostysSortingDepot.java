package net.frostytrix.sortingdepot;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.frostytrix.sortingdepot.datagen.SDDataGenerators;
import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.frostytrix.sortingdepot.registry.SDBlocks;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.frostytrix.sortingdepot.registry.SDItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(FrostysSortingDepot.MOD_ID)
public class FrostysSortingDepot {

    public static final String MOD_ID = "frostyssortingdepot";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FrostysSortingDepot(IEventBus modEventBus, ModContainer modContainer) {
        // Data components must exist for items to reference them, so register them first.
        SDDataComponents.register(modEventBus);
        SDBlocks.register(modEventBus);
        SDBlockEntities.register(modEventBus);
        SDItems.register(modEventBus);

        // Data generation (runData).
        modEventBus.addListener(SDDataGenerators::gatherData);
    }
}
