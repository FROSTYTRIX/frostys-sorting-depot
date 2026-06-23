package net.frostytrix.sortingdepot.datagen;

import java.util.Set;
import java.util.stream.Collectors;

import net.frostytrix.sortingdepot.registry.SDBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

/**
 * Block loot: every block simply drops itself. Inventory contents are spilled separately by each block
 * entity's {@code preRemoveSideEffects}, since our inventories use the transfer API rather than a vanilla
 * {@code Container}.
 */
public class SDBlockLoot extends BlockLootSubProvider {

    protected SDBlockLoot(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.VANILLA_SET, registries);
    }

    @Override
    protected void generate() {
        dropSelf(SDBlocks.LINKER_NODE.get());
        dropSelf(SDBlocks.DEPOT_CONTROLLER.get());
        dropSelf(SDBlocks.OVERFLOW_CHEST.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return SDBlocks.BLOCKS.getEntries().stream().map(holder -> (Block) holder.get()).collect(Collectors.toList());
    }
}
