package net.frostytrix.sortingdepot.datagen;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.registry.SDBlocks;
import net.frostytrix.sortingdepot.registry.SDItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TexturedModel;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

/**
 * Generates item models (and the 1.21.4+ item-model definitions) for every item the mod adds.
 *
 * <p>Each is a flat ("generated") model whose texture is derived from the item id, i.e.
 * {@code assets/frostyssortingdepot/textures/item/<name>.png}. {@link ModelProvider} validates that
 * every item in our namespace is covered here, so this list must stay in sync with {@link SDItems}.
 */
public class SDItemModelProvider extends ModelProvider {

    public SDItemModelProvider(PackOutput output) {
        super(output, FrostysSortingDepot.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // Items.
        itemModels.generateFlatItem(SDItems.FILTER_CARD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(SDItems.PRIORITY_STAMP.get(), ModelTemplates.FLAT_ITEM);
        // The Linker is a handheld tool, so it renders in-hand like a sword/wand.
        itemModels.generateFlatItem(SDItems.LINKER.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        // Blocks (also generates their block-item models).
        // Linker Node + Depot Terminal face a direction → orientable model (front/side/top), rotated by facing.
        blockModels.createHorizontallyRotatedBlock(SDBlocks.LINKER_NODE.get(), TexturedModel.ORIENTABLE_ONLY_TOP);
        blockModels.createHorizontallyRotatedBlock(SDBlocks.DEPOT_TERMINAL.get(), TexturedModel.ORIENTABLE_ONLY_TOP);
        // Depot Controller: distinct top (intake) + sides + bottom.
        blockModels.createTrivialBlock(SDBlocks.DEPOT_CONTROLLER.get(), TexturedModel.CUBE_TOP_BOTTOM);
        // Overflow Chest is an entity-rendered chest: particle-only blockstate + a 3D chest item model.
        blockModels.createChest(SDBlocks.OVERFLOW_CHEST.get(), Blocks.OAK_PLANKS,
                Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "entity/chest/overflow"), false);
    }
}
