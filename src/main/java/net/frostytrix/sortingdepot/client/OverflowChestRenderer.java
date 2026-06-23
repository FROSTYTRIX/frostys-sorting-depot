package net.frostytrix.sortingdepot.client;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.blockentity.OverflowChestBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Renders the Overflow Chest with the mod's custom chest texture (sprites in the vanilla chest atlas)
 * while reusing the vanilla {@link ChestRenderer} for the model and lid animation. A double chest uses
 * distinct left/right halves, exactly like vanilla's {@code normal_left}/{@code normal_right}.
 */
public class OverflowChestRenderer extends ChestRenderer<OverflowChestBlockEntity> {

    private static final SpriteId SINGLE = sprite("entity/chest/overflow");
    private static final SpriteId LEFT = sprite("entity/chest/overflow_left");
    private static final SpriteId RIGHT = sprite("entity/chest/overflow_right");

    private static SpriteId sprite(String path) {
        return new SpriteId(Sheets.CHEST_SHEET, Identifier.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, path));
    }

    public OverflowChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SpriteId getCustomSprite(OverflowChestBlockEntity blockEntity, ChestRenderState renderState) {
        return switch (renderState.type) {
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            default -> SINGLE;
        };
    }
}
