package net.frostytrix.sortingdepot.datagen;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.frostytrix.sortingdepot.registry.SDItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Generates the mod's crafting recipes. The three Filter Card recipes all output the same
 * {@code filter_card} item but bake a different {@link FilterCardData} into the result via an
 * {@link ItemStackTemplate}, so each crafts a card pre-set to Item / Tag / Overflow mode.
 */
public class SDRecipeProvider extends RecipeProvider {

    protected SDRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        // Filter Card (Item mode) — plain card, no baked component (defaults to EMPTY = Item mode).
        shaped(RecipeCategory.MISC, new ItemStackTemplate(SDItems.FILTER_CARD.get()))
                .pattern("PN")
                .define('P', Items.PAPER)
                .define('N', Items.IRON_NUGGET)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output, key("filter_card_item"));

        // Filter Card (Tag mode) — Paper + Gold Nugget.
        shaped(RecipeCategory.MISC, card(FilterCardData.Mode.TAG))
                .pattern("PN")
                .define('P', Items.PAPER)
                .define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_gold_nugget", has(Items.GOLD_NUGGET))
                .save(output, key("filter_card_tag"));

        // Filter Card (Overflow mode) — Paper only.
        shaped(RecipeCategory.MISC, card(FilterCardData.Mode.OVERFLOW))
                .pattern("P")
                .define('P', Items.PAPER)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output, key("filter_card_overflow"));

        // Priority Stamp — Iron Ingot + Redstone Torch.
        shaped(RecipeCategory.MISC, new ItemStackTemplate(SDItems.PRIORITY_STAMP.get()))
                .pattern("T")
                .pattern("I")
                .define('T', Items.REDSTONE_TORCH)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_redstone_torch", has(Items.REDSTONE_TORCH))
                .save(output, key("priority_stamp"));

        // Linker Node — Iron + Redstone (cheap; one per destination).
        shaped(RecipeCategory.MISC, new ItemStackTemplate(SDItems.LINKER_NODE.get()))
                .pattern("III")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output, key("linker_node"));

        // Depot Controller — Iron Block + Hopper + Comparator (mid-game hub).
        shaped(RecipeCategory.MISC, new ItemStackTemplate(SDItems.DEPOT_CONTROLLER.get()))
                .pattern("H")
                .pattern("B")
                .pattern("C")
                .define('H', Items.HOPPER)
                .define('B', Items.IRON_BLOCK)
                .define('C', Items.COMPARATOR)
                .unlockedBy("has_comparator", has(Items.COMPARATOR))
                .save(output, key("depot_controller"));

        // Depot Terminal — Controller + Glass Pane + Redstone (optional QoL).
        shaped(RecipeCategory.MISC, new ItemStackTemplate(SDItems.DEPOT_TERMINAL.get()))
                .pattern("R")
                .pattern("G")
                .pattern("D")
                .define('R', Items.REDSTONE)
                .define('G', Items.GLASS_PANE)
                .define('D', SDItems.DEPOT_CONTROLLER.get())
                .unlockedBy("has_depot_controller", has(SDItems.DEPOT_CONTROLLER.get()))
                .save(output, key("depot_terminal"));

        // Overflow Chest — Chest + Iron Ingot + Hopper.
        shaped(RecipeCategory.MISC, new ItemStackTemplate(SDItems.OVERFLOW_CHEST.get()))
                .pattern("C")
                .pattern("I")
                .pattern("H")
                .define('C', Items.CHEST)
                .define('I', Items.IRON_INGOT)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, key("overflow_chest"));

        // Linker — Iron Ingot + Redstone + Stick (cheap, players need many).
        shaped(RecipeCategory.MISC, new ItemStackTemplate(SDItems.LINKER.get()))
                .pattern("I")
                .pattern("R")
                .pattern("S")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('S', Items.STICK)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output, key("linker"));
    }

    /** A filter_card result template pre-configured to the given mode. */
    private static ItemStackTemplate card(FilterCardData.Mode mode) {
        FilterCardData data = new FilterCardData(mode, Optional.empty(), Set.of(), false);
        DataComponentPatch patch = DataComponentPatch.builder()
                .set(SDDataComponents.FILTER_DATA.get(), data)
                .build();
        return new ItemStackTemplate(SDItems.FILTER_CARD.get(), patch);
    }

    private static ResourceKey<Recipe<?>> key(String name) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, name));
    }

    /** Wires the provider into NeoForge's data generation. */
    public static class Runner extends RecipeProvider.Runner {

        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new SDRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Sorting Depot Recipes";
        }
    }
}
