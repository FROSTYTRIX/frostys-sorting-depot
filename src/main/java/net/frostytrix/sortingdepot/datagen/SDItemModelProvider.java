package net.frostytrix.sortingdepot.datagen;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.registry.SDBlocks;
import net.frostytrix.sortingdepot.registry.SDItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
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

        // Blocks. These three keep hand-authored models in src/main/resources, so datagen generates only
        // the blockstate + block-item model that point at them — it never writes the block model itself.
        handModeledBlock(blockModels, SDBlocks.LINKER_NODE.get(), true);
        handModeledBlock(blockModels, SDBlocks.DEPOT_TERMINAL.get(), true);
        handModeledBlock(blockModels, SDBlocks.DEPOT_CONTROLLER.get(), false);

        // Overflow Chest is an entity-rendered chest: particle-only blockstate + a 3D chest item model.
        blockModels.createChest(SDBlocks.OVERFLOW_CHEST.get(), Blocks.OAK_PLANKS,
                Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "entity/chest/overflow"), false);
    }

    /**
     * Generates the blockstate and block-item model for a block whose model is authored by hand (kept in
     * {@code src/main/resources}). Optionally rotates the blockstate by {@code facing}.
     */
    private static void handModeledBlock(BlockModelGenerators blockModels, Block block, boolean facing) {
        Identifier model = ModelLocationUtils.getModelLocation(block);
        MultiVariant variant = BlockModelGenerators.plainVariant(model);
        if (facing) {
            blockModels.blockStateOutput.accept(
                    MultiVariantGenerator.dispatch(block, variant).with(BlockModelGenerators.ROTATION_HORIZONTAL_FACING));
        } else {
            blockModels.blockStateOutput.accept(BlockModelGenerators.createSimpleBlock(block, variant));
        }
        blockModels.registerSimpleItemModel(block, model);
    }
}
