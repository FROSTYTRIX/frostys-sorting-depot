package net.frostytrix.sortingdepot;

import net.frostytrix.sortingdepot.gui.DepotControllerScreen;
import net.frostytrix.sortingdepot.gui.DepotTerminalScreen;
import net.frostytrix.sortingdepot.gui.LinkerNodeScreen;
import net.frostytrix.sortingdepot.gui.OverflowChestScreen;
import net.frostytrix.sortingdepot.registry.SDMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = FrostysSortingDepot.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = FrostysSortingDepot.MOD_ID, value = Dist.CLIENT)
public class FrostysSortingDepotClient {
    public FrostysSortingDepotClient(ModContainer container) {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(SDMenus.LINKER_NODE.get(), LinkerNodeScreen::new);
        event.register(SDMenus.OVERFLOW_CHEST.get(), OverflowChestScreen::new);
        event.register(SDMenus.DEPOT_CONTROLLER.get(), DepotControllerScreen::new);
        event.register(SDMenus.DEPOT_TERMINAL.get(), DepotTerminalScreen::new);
    }
}
