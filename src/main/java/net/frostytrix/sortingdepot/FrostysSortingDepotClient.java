package net.frostytrix.sortingdepot;

import net.frostytrix.sortingdepot.client.OverflowChestRenderer;
import net.frostytrix.sortingdepot.client.SDLinkerBeams;
import net.frostytrix.sortingdepot.gui.DepotControllerScreen;
import net.frostytrix.sortingdepot.gui.DepotTerminalScreen;
import net.frostytrix.sortingdepot.gui.FilterCardScreen;
import net.frostytrix.sortingdepot.gui.LinkerNodeScreen;
import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.frostytrix.sortingdepot.registry.SDMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = FrostysSortingDepot.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = FrostysSortingDepot.MOD_ID, value = Dist.CLIENT)
public class FrostysSortingDepotClient {
    public FrostysSortingDepotClient(ModContainer container) {
        // World-overlay beams render on the game bus, not the mod bus.
        NeoForge.EVENT_BUS.addListener(
                (RenderLevelStageEvent.AfterTranslucentBlocks event) -> SDLinkerBeams.onRenderLevelStage(event));
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        // The Overflow Chest now uses the vanilla chest screen (no custom screen needed).
        event.register(SDMenus.LINKER_NODE.get(), LinkerNodeScreen::new);
        event.register(SDMenus.FILTER_CARD.get(), FilterCardScreen::new);
        event.register(SDMenus.DEPOT_CONTROLLER.get(), DepotControllerScreen::new);
        event.register(SDMenus.DEPOT_TERMINAL.get(), DepotTerminalScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SDBlockEntities.OVERFLOW_CHEST.get(), OverflowChestRenderer::new);
    }
}
