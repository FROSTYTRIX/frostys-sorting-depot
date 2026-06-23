package net.frostytrix.sortingdepot.datagen;

import java.util.List;
import java.util.Set;

import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
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
        event.createProvider((output, lookup) -> new LootTableProvider(
                output,
                Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(SDBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
        event.createProvider((output, lookup) -> new AdvancementProvider(output, lookup, List.of(new SDAdvancements())));
    }
}
