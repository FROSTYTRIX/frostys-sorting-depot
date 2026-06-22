package net.frostytrix.sortingdepot.registry;

import java.util.function.Supplier;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for the mod's data component types. All components are persistent (saved to NBT) and
 * network-synchronized (sent to the client for tooltips/rendering).
 */
public final class SDDataComponents {

    private SDDataComponents() {
    }

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, FrostysSortingDepot.MOD_ID);

    /** Filter configuration stored on a Filter Card. */
    public static final Supplier<DataComponentType<FilterCardData>> FILTER_DATA =
            DATA_COMPONENTS.register("filter_data", () -> DataComponentType.<FilterCardData>builder()
                    .persistent(FilterCardData.CODEC)
                    .networkSynchronized(FilterCardData.STREAM_CODEC)
                    .build());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
