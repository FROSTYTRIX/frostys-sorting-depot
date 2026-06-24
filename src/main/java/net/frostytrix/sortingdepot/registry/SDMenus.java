package net.frostytrix.sortingdepot.registry;

import java.util.function.Supplier;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.gui.DepotControllerMenu;
import net.frostytrix.sortingdepot.gui.DepotTerminalMenu;
import net.frostytrix.sortingdepot.gui.FilterCardMenu;
import net.frostytrix.sortingdepot.gui.LinkerNodeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Menu types for the mod's GUIs.
 */
public final class SDMenus {

    private SDMenus() {
    }

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, FrostysSortingDepot.MOD_ID);

    public static final Supplier<MenuType<LinkerNodeMenu>> LINKER_NODE =
            MENUS.register("linker_node", () -> IMenuTypeExtension.create(LinkerNodeMenu::new));

    public static final Supplier<MenuType<FilterCardMenu>> FILTER_CARD =
            MENUS.register("filter_card", () -> IMenuTypeExtension.create(FilterCardMenu::new));

    public static final Supplier<MenuType<DepotControllerMenu>> DEPOT_CONTROLLER =
            MENUS.register("depot_controller", () -> IMenuTypeExtension.create(DepotControllerMenu::new));

    public static final Supplier<MenuType<DepotTerminalMenu>> DEPOT_TERMINAL =
            MENUS.register("depot_terminal", () -> IMenuTypeExtension.create(DepotTerminalMenu::new));

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
