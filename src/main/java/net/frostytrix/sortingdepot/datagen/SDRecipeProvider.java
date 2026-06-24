package net.frostytrix.sortingdepot.datagen;

import java.util.concurrent.CompletableFuture;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.registry.SDItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/**
 * Generates the mod's crafting recipes. 1.21.1's {@link RecipeProvider} ctor takes
 * {@code (PackOutput, CompletableFuture<HolderLookup.Provider>)} and {@code save} takes a
 * {@link ResourceLocation} rather than a {@code ResourceKey<Recipe<?>>}. A single {@code filter_card}
 * recipe outputs a blank card; configuration happens at runtime in the Filter Card GUI.
 */
public class SDRecipeProvider extends RecipeProvider {

    public SDRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SDItems.FILTER_CARD.get())
                .pattern("PN")
                .define('P', Items.PAPER)
                .define('N', Items.IRON_NUGGET)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(output, key("filter_card_item"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SDItems.PRIORITY_STAMP.get())
                .pattern("T")
                .pattern("I")
                .define('T', Items.REDSTONE_TORCH)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_redstone_torch", has(Items.REDSTONE_TORCH))
                .save(output, key("priority_stamp"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SDItems.LINKER_NODE.get())
                .pattern("III")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output, key("linker_node"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SDItems.DEPOT_CONTROLLER.get())
                .pattern("H")
                .pattern("B")
                .pattern("C")
                .define('H', Items.HOPPER)
                .define('B', Items.IRON_BLOCK)
                .define('C', Items.COMPARATOR)
                .unlockedBy("has_comparator", has(Items.COMPARATOR))
                .save(output, key("depot_controller"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SDItems.DEPOT_TERMINAL.get())
                .pattern("R")
                .pattern("G")
                .pattern("D")
                .define('R', Items.REDSTONE)
                .define('G', Items.GLASS_PANE)
                .define('D', SDItems.DEPOT_CONTROLLER.get())
                .unlockedBy("has_depot_controller", has(SDItems.DEPOT_CONTROLLER.get()))
                .save(output, key("depot_terminal"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SDItems.OVERFLOW_CHEST.get())
                .pattern("C")
                .pattern("I")
                .pattern("H")
                .define('C', Items.CHEST)
                .define('I', Items.IRON_INGOT)
                .define('H', Items.HOPPER)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(output, key("overflow_chest"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SDItems.LINKER.get())
                .pattern("I")
                .pattern("R")
                .pattern("S")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('S', Items.STICK)
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(output, key("linker"));
    }

    private static ResourceLocation key(String name) {
        return ResourceLocation.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, name);
    }
}
