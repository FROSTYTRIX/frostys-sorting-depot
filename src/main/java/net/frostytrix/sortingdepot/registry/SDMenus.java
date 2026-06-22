package net.frostytrix.sortingdepot.registry;

import java.util.function.Supplier;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
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

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
