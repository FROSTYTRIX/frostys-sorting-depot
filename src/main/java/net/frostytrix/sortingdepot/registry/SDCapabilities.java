package net.frostytrix.sortingdepot.registry;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

/**
 * Capability wiring. The Controller exposes its input buffer as the item capability on the top face so
 * hoppers, droppers, and pipes can feed it.
 */
public final class SDCapabilities {

    private SDCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        // Controller: input buffer on the top face.
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                SDBlockEntities.DEPOT_CONTROLLER.get(),
                (be, side) -> (side == null || side == Direction.UP) ? be.getInputHandler() : null);

        // Overflow Chest (a vanilla-style Container): expose its inventory on every face.
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                SDBlockEntities.OVERFLOW_CHEST.get(),
                (be, side) -> VanillaContainerWrapper.of(be));
    }
}
