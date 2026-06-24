package net.frostytrix.sortingdepot.datagen;

import java.util.List;
import java.util.Set;

import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for {@code runData} on 1.21.1.
 *
 * <p>On 1.21.1 {@link GatherDataEvent} is a single event (not split into Client/Server). Models for
 * blocks and items are shipped as <em>static</em> resources in {@code src/main/resources/assets/...},
 * so no model provider is registered here — only recipes, loot, and advancements.
 */
public final class SDDataGenerators {

    private SDDataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        PackOutput output = event.getGenerator().getPackOutput();
        var lookup = event.getLookupProvider();

        event.getGenerator().addProvider(event.includeServer(),
                new SDRecipeProvider(output, lookup));
        event.getGenerator().addProvider(event.includeServer(),
                new LootTableProvider(
                        output,
                        Set.of(),
                        List.of(new LootTableProvider.SubProviderEntry(SDBlockLoot::new, LootContextParamSets.BLOCK)),
                        lookup));
        event.getGenerator().addProvider(event.includeServer(),
                new AdvancementProvider(output, lookup, List.of(new SDAdvancements())));
    }
}
