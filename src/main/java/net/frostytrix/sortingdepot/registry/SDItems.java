package net.frostytrix.sortingdepot.registry;

import java.util.function.Supplier;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.item.FilterCardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All of the mod's items and its creative tab. Items are never registered ad hoc — they live here.
 */
public final class SDItems {

    private SDItems() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FrostysSortingDepot.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FrostysSortingDepot.MOD_ID);

    public static final DeferredItem<FilterCardItem> FILTER_CARD =
            ITEMS.registerItem("filter_card", FilterCardItem::new);

    /** Single creative tab holding everything the mod adds. */
    public static final Supplier<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.frostyssortingdepot"))
            .icon(() -> FILTER_CARD.get().getDefaultInstance())
            .displayItems((parameters, output) -> output.accept(FILTER_CARD.get()))
            .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
    }
}
