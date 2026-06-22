package net.frostytrix.sortingdepot.datagen;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.registry.SDItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

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
        itemModels.generateFlatItem(SDItems.FILTER_CARD.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(SDItems.PRIORITY_STAMP.get(), ModelTemplates.FLAT_ITEM);
        itemModels.generateFlatItem(SDItems.LINKER.get(), ModelTemplates.FLAT_ITEM);
    }
}
