package net.frostytrix.sortingdepot.datagen;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for {@code runData}. Registers every data provider the mod ships.
 */
public final class SDDataGenerators {

    private SDDataGenerators() {
    }

    // 26.2 split GatherDataEvent into Client/Server; the `data` run uses clientData() → the Client event.
    public static void gatherData(GatherDataEvent.Client event) {
        event.createProvider(SDRecipeProvider.Runner::new);
        event.createProvider(SDItemModelProvider::new);
    }
}
