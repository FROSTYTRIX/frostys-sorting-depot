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
 * The "Sorting Depot" advancement tree, organised as a step-by-step tutorial. Each step's description
 * explains what to do next, so following the chain teaches the mod's flow: craft the Controller, place
 * a Linker Node, get a Linker tool, slot a Filter Card, register the destination, and finally place an
 * Overflow Chest + Terminal.
 */
public class SDAdvancements implements AdvancementSubProvider {

    private static final String PREFIX = "advancements.frostyssortingdepot.";

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {
        // root: craft the Controller — start of the chain.
        AdvancementHolder root = Advancement.Builder.advancement()
                .display(SDItems.DEPOT_CONTROLLER.get(),
                        Component.translatable(PREFIX + "root.title"),
                        Component.translatable(PREFIX + "root.description"),
                        ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png"),
                        AdvancementType.TASK, true, true, false)
                .addCriterion("has_controller",
                        InventoryChangeTrigger.TriggerInstance.hasItems(SDItems.DEPOT_CONTROLLER.get()))
                .save(saver, FrostysSortingDepot.MOD_ID + ":sorting_depot/root");

        // 2. Place a destination marker — the Linker Node — which is the actual block items will pass
        //    through. Tutorial mentions placing it against a chest.
        AdvancementHolder node = child(saver, root, SDItems.LINKER_NODE.get(), "node", AdvancementType.TASK);

        // 3. Craft a Linker tool — needed to register the node into a Controller's network.
        AdvancementHolder linker = child(saver, node, SDItems.LINKER.get(), "linker", AdvancementType.TASK);

        // 4. Craft a Filter Card — the actual "what does this node accept" piece. Right-click in air
        //    to configure (text describes the modes).
        AdvancementHolder filter = child(saver, linker, SDItems.FILTER_CARD.get(), "filter", AdvancementType.TASK);

        // 5. Optional sibling on the Filter row: Priority Stamp tunes how multiple destinations rank.
        child(saver, filter, SDItems.PRIORITY_STAMP.get(), "priority", AdvancementType.TASK);

        // 6. Safety net: an Overflow Chest catches anything no destination accepted.
        AdvancementHolder overflow = child(saver, filter, SDItems.OVERFLOW_CHEST.get(), "overflow", AdvancementType.TASK);

        // 7. Final goal: the Terminal — the live dashboard. Marked as GOAL so it lights up as the end
        //    of the chain.
        child(saver, overflow, SDItems.DEPOT_TERMINAL.get(), "terminal", AdvancementType.GOAL);
    }

    private static AdvancementHolder child(Consumer<AdvancementHolder> saver, AdvancementHolder parent,
                                           Item icon, String key, AdvancementType type) {
        return Advancement.Builder.advancement()
                .parent(parent)
                .display(icon,
                        Component.translatable(PREFIX + key + ".title"),
                        Component.translatable(PREFIX + key + ".description"),
                        null, type, true, true, false)
                .addCriterion("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(icon))
                .save(saver, FrostysSortingDepot.MOD_ID + ":sorting_depot/" + key);
    }
}
