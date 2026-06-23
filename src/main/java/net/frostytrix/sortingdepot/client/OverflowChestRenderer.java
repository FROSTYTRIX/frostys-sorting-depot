package net.frostytrix.sortingdepot.client;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.blockentity.OverflowChestBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

/**
 * Renders the Overflow Chest with the mod's custom chest texture (a sprite in the vanilla chest atlas)
 * while reusing the vanilla {@link ChestRenderer} for the model and lid animation.
 */
public class OverflowChestRenderer extends ChestRenderer<OverflowChestBlockEntity> {

    private static final SpriteId SPRITE = new SpriteId(
            Sheets.CHEST_SHEET,
            Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, "entity/chest/overflow"));

    public OverflowChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SpriteId getCustomSprite(OverflowChestBlockEntity blockEntity, ChestRenderState renderState) {
        return SPRITE;
    }
}
