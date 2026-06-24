package net.frostytrix.sortingdepot.client;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.blockentity.OverflowChestBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Renders the Overflow Chest with the mod's custom chest texture (sprites in the vanilla chest atlas)
 * while reusing the vanilla {@link ChestRenderer} for the model and lid animation. A double chest uses
 * distinct left/right halves, exactly like vanilla's {@code normal_left}/{@code normal_right}.
 *
 * <p>{@link Sheets#CHEST_MAPPER} prepends {@code entity/chest/}, so the short sprite names are passed.
 */
public class OverflowChestRenderer extends ChestRenderer<OverflowChestBlockEntity> {

    private static final Material SINGLE = material("overflow");
    private static final Material LEFT = material("overflow_left");
    private static final Material RIGHT = material("overflow_right");

    private static Material material(String shortName) {
        return Sheets.CHEST_MAPPER.apply(ResourceLocation.fromNamespaceAndPath(FrostysSortingDepot.MOD_ID, shortName));
    }

    public OverflowChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Material getMaterial(OverflowChestBlockEntity blockEntity, ChestType type) {
        return switch (type) {
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            default -> SINGLE;
        };
    }
}
