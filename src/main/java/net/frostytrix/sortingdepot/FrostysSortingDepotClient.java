package net.frostytrix.sortingdepot;

import net.frostytrix.sortingdepot.client.OverflowChestRenderer;
import net.frostytrix.sortingdepot.client.SDKeyMappings;
import net.frostytrix.sortingdepot.client.SDLinkerBeams;
import net.frostytrix.sortingdepot.gui.DepotControllerScreen;
import net.frostytrix.sortingdepot.gui.DepotTerminalScreen;
import net.frostytrix.sortingdepot.gui.FilterCardScreen;
import net.frostytrix.sortingdepot.gui.LinkerNodeScreen;
import net.frostytrix.sortingdepot.registry.SDBlockEntities;
import net.frostytrix.sortingdepot.registry.SDMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = FrostysSortingDepot.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = FrostysSortingDepot.MOD_ID, value = Dist.CLIENT)
public class FrostysSortingDepotClient {
    public FrostysSortingDepotClient(ModContainer container) {
        // Client-only visual settings (Linker beam colour/width/toggle) + its in-game config screen.
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new ConfigurationScreen(modContainer, parent));

        // World-overlay beams + keybind polling render/run on the game bus, not the mod bus.
        NeoForge.EVENT_BUS.addListener(
                (RenderLevelStageEvent.AfterTranslucentBlocks event) -> SDLinkerBeams.onRenderLevelStage(event));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> pollKeys());
    }

    private static void pollKeys() {
        while (SDKeyMappings.TOGGLE_WIRING.consumeClick()) {
            SDLinkerBeams.toggleWiring();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable(SDLinkerBeams.wiringActive()
                        ? "gui.frostyssortingdepot.wiring.on"
                        : "gui.frostyssortingdepot.wiring.off"), true);
            }
        }
    }

    @SubscribeEvent
    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SDKeyMappings.TOGGLE_WIRING);
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
