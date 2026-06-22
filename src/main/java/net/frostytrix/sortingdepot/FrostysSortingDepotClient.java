package net.frostytrix.sortingdepot;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = FrostysSortingDepot.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = FrostysSortingDepot.MOD_ID, value = Dist.CLIENT)
public class FrostysSortingDepotClient {
    public FrostysSortingDepotClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }
}
