package net.frostytrix.sortingdepot.registry;

import java.util.function.Supplier;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.item.FilterCardItem;
import net.frostytrix.sortingdepot.item.LinkerItem;
import net.frostytrix.sortingdepot.item.PriorityStampItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
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

    // Stacks to 1: each card carries its own filter configuration, so they must stay individually editable.
    public static final DeferredItem<FilterCardItem> FILTER_CARD =
            ITEMS.registerItem("filter_card", FilterCardItem::new, props -> props.stacksTo(1));
    public static final DeferredItem<PriorityStampItem> PRIORITY_STAMP =
            ITEMS.registerItem("priority_stamp", PriorityStampItem::new);
    public static final DeferredItem<LinkerItem> LINKER =
            ITEMS.registerItem("linker", LinkerItem::new);

    // Block items. Registered here (not in SDBlocks) so all item-registry entries live in one place.
    public static final DeferredItem<BlockItem> LINKER_NODE =
            ITEMS.registerSimpleBlockItem("linker_node", SDBlocks.LINKER_NODE);
    public static final DeferredItem<BlockItem> DEPOT_CONTROLLER =
            ITEMS.registerSimpleBlockItem("depot_controller", SDBlocks.DEPOT_CONTROLLER);
    public static final DeferredItem<BlockItem> OVERFLOW_CHEST =
            ITEMS.registerSimpleBlockItem("overflow_chest", SDBlocks.OVERFLOW_CHEST);
    public static final DeferredItem<BlockItem> DEPOT_TERMINAL =
            ITEMS.registerSimpleBlockItem("depot_terminal", SDBlocks.DEPOT_TERMINAL);

    /** Single creative tab holding everything the mod adds. */
    public static final Supplier<CreativeModeTab> MAIN_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.frostyssortingdepot"))
            .icon(() -> FILTER_CARD.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(FILTER_CARD.get());
                output.accept(PRIORITY_STAMP.get());
                output.accept(LINKER.get());
                output.accept(LINKER_NODE.get());
                output.accept(DEPOT_CONTROLLER.get());
                output.accept(OVERFLOW_CHEST.get());
                output.accept(DEPOT_TERMINAL.get());
            })
            .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
    }
}
