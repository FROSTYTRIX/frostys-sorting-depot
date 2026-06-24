package net.frostytrix.sortingdepot.datagen;

import java.util.function.Consumer;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.registry.SDItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * The "Sorting Depot" advancement tree: craft the Controller (root), then a Filter Card, a Linker, an
 * Overflow Chest, and a Depot Terminal. Each is a simple "obtain the item" milestone.
 */
public class SDAdvancements implements AdvancementSubProvider {

    private static final String PREFIX = "advancements.frostyssortingdepot.";

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(SDItems.DEPOT_CONTROLLER.get(),
                        Component.translatable(PREFIX + "root.title"),
                        Component.translatable(PREFIX + "root.description"),
                        ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                        AdvancementType.TASK, true, true, false)
                .addCriterion("has_controller",
                        InventoryChangeTrigger.TriggerInstance.hasItems(SDItems.DEPOT_CONTROLLER.get()))
                .save(saver, FrostysSortingDepot.MOD_ID + ":sorting_depot/root");

        child(saver, root, SDItems.FILTER_CARD.get(), "filter", AdvancementType.TASK);
        child(saver, root, SDItems.LINKER.get(), "linker", AdvancementType.TASK);
        child(saver, root, SDItems.OVERFLOW_CHEST.get(), "overflow", AdvancementType.TASK);
        child(saver, root, SDItems.DEPOT_TERMINAL.get(), "terminal", AdvancementType.GOAL);
    }

    private static void child(Consumer<AdvancementHolder> saver, AdvancementHolder parent, Item icon,
                              String key, AdvancementType type) {
        Advancement.Builder.advancement()
                .parent(parent)
                .display(icon,
                        Component.translatable(PREFIX + key + ".title"),
                        Component.translatable(PREFIX + key + ".description"),
                        null, type, true, true, false)
                .addCriterion("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(icon))
                .save(saver, FrostysSortingDepot.MOD_ID + ":sorting_depot/" + key);
    }
}
