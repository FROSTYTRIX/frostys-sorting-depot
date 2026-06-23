package net.frostytrix.sortingdepot.registry;

import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Capability wiring. The Controller exposes its input buffer as the item capability on the top face so
 * hoppers, droppers, and pipes can feed it.
 */
public final class SDCapabilities {

    private SDCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Item.BLOCK,
                SDBlockEntities.DEPOT_CONTROLLER.get(),
                (be, side) -> (side == null || side == Direction.UP) ? be.getInputHandler() : null);
    }
}
