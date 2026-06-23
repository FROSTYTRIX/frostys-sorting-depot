package net.frostytrix.sortingdepot.debug;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.frostytrix.sortingdepot.FrostysSortingDepot;
import net.frostytrix.sortingdepot.block.LinkerNodeBlock;
import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.frostytrix.sortingdepot.blockentity.LinkerNodeBlockEntity;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.frostytrix.sortingdepot.registry.SDBlocks;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.frostytrix.sortingdepot.registry.SDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;

/**
 * Dev-only in-world smoke test for the routing pipeline. Runs on server start when
 * {@code -Dsd.selftest=true} is set (e.g. the {@code runServer} dev config), never in production.
 *
 * <p>It builds a Controller + Linker Node + chest, feeds the Controller a stack matching the Linker's
 * Filter Card, drives the routing ticks, and logs whether the items reached the chest.
 */
@EventBusSubscriber(modid = FrostysSortingDepot.MOD_ID)
public final class SelfTest {

    private static final int TEST_COUNT = 16;

    private SelfTest() {
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        if (!"true".equals(System.getProperty("sd.selftest"))) {
            return;
        }
        try {
            run(event.getServer().overworld());
        } catch (Throwable t) {
            FrostysSortingDepot.LOGGER.error("[SD-SELFTEST] CRASHED", t);
        }
    }

    private static void run(ServerLevel level) {
        BlockPos chestPos = new BlockPos(8, 120, 8);
        BlockPos linkerPos = chestPos.west();        // Linker Node sits west of the chest...
        BlockPos controllerPos = chestPos.above(2);
        BlockPos overflowPos = controllerPos.north(); // Overflow Chest adjacent to the Controller.
        List<BlockPos> all = List.of(chestPos, linkerPos, controllerPos, overflowPos);

        all.forEach(p -> level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));

        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        // ...and faces EAST, toward the chest, so its target inventory resolves to the chest.
        level.setBlockAndUpdate(linkerPos,
                SDBlocks.LINKER_NODE.get().defaultBlockState().setValue(LinkerNodeBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(controllerPos, SDBlocks.DEPOT_CONTROLLER.get().defaultBlockState());
        level.setBlockAndUpdate(overflowPos, SDBlocks.OVERFLOW_CHEST.get().defaultBlockState());

        if (!(level.getBlockEntity(linkerPos) instanceof LinkerNodeBlockEntity node)) {
            fail("Linker Node block entity missing", all, level);
            return;
        }
        if (!(level.getBlockEntity(controllerPos) instanceof DepotControllerBlockEntity controller)) {
            fail("Depot Controller block entity missing", all, level);
            return;
        }

        // Configure the Linker's Filter Card to accept cobblestone, then register it with the Controller.
        ItemStack card = new ItemStack(SDItems.FILTER_CARD.get());
        card.set(SDDataComponents.FILTER_DATA.get(), FilterCardData.ofItem(Identifier.parse("minecraft:cobblestone"), false));
        node.getFilterSlot().set(0, ItemResource.of(card), 1);
        controller.addLinker(linkerPos);

        BlockState controllerState = level.getBlockState(controllerPos);

        // Scenario 1: a matching item routes to the linked chest.
        feedAndRoute(level, controller, controllerPos, controllerState, Items.COBBLESTONE);
        int inChest = countItem(level, chestPos, Items.COBBLESTONE);
        int afterMatch = ItemUtil.getStack(controller.getInputHandler(), 0).getCount();

        // Scenario 1b: a TAG filter routes by tag. Rebind the card to #minecraft:logs and feed oak logs.
        ItemStack tagCard = new ItemStack(SDItems.FILTER_CARD.get());
        tagCard.set(SDDataComponents.FILTER_DATA.get(), new FilterCardData(
                FilterCardData.Mode.TAG, Optional.empty(), Set.of(Identifier.parse("minecraft:logs")), false));
        node.getFilterSlot().set(0, ItemResource.of(tagCard), 1);
        feedAndRoute(level, controller, controllerPos, controllerState, Items.OAK_LOG);
        int logsInChest = countItem(level, chestPos, Items.OAK_LOG);

        // Scenario 2: a non-matching item falls through to the Overflow Chest.
        feedAndRoute(level, controller, controllerPos, controllerState, Items.DIRT);
        int inOverflow = countItem(level, overflowPos, Items.DIRT);
        int afterOverflow = ItemUtil.getStack(controller.getInputHandler(), 0).getCount();

        // Scenario 3: tear the whole network down, then feed a fresh item. With nothing able to accept it,
        // it must stay safely in the buffer — never voided. (Content-drop-on-break is handled by each block
        // entity's preRemoveSideEffects; it can't be asserted here because this runs before the world ticks,
        // so dropped item entities aren't indexed yet — verify it in-game by breaking a filled block.)
        level.setBlockAndUpdate(linkerPos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(chestPos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(overflowPos, Blocks.AIR.defaultBlockState());
        feedAndRoute(level, controller, controllerPos, controllerState, Items.GOLD_INGOT);
        int strandedInInput = ItemUtil.getStack(controller.getInputHandler(), 0).getCount();

        boolean matchOk = inChest == TEST_COUNT && afterMatch == 0;
        boolean tagOk = logsInChest == TEST_COUNT;
        boolean overflowOk = inOverflow == TEST_COUNT && afterOverflow == 0;
        boolean noVoidOk = strandedInInput == TEST_COUNT;
        if (matchOk && tagOk && overflowOk && noVoidOk) {
            FrostysSortingDepot.LOGGER.info(
                    "[SD-SELFTEST] PASS: item-filter->chest ({}), tag-filter->chest ({}), "
                            + "unmatched->overflow ({}), no-target items stay buffered ({}, never voided)",
                    inChest, logsInChest, inOverflow, strandedInInput);
        } else {
            fail("item{chest=" + inChest + ",input=" + afterMatch + "} tag{logs=" + logsInChest + "/" + TEST_COUNT
                    + "} overflow{chest=" + inOverflow + ",input=" + afterOverflow + "} "
                    + "novoid{buffered=" + strandedInInput + "/" + TEST_COUNT + "}", all, level);
        }

        all.forEach(p -> level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
    }

    private static void feedAndRoute(ServerLevel level, DepotControllerBlockEntity controller,
                                     BlockPos controllerPos, BlockState controllerState, net.minecraft.world.item.Item item) {
        controller.getInputHandler().set(0, ItemResource.of(new ItemStack(item)), TEST_COUNT);
        for (int i = 0; i < 20; i++) {
            DepotControllerBlockEntity.serverTick(level, controllerPos, controllerState, controller);
        }
    }

    private static int countItem(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        if (handler == null) {
            return -1;
        }
        int total = 0;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemStack stack = ItemUtil.getStack(handler, slot);
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void fail(String reason, List<BlockPos> cleanup, ServerLevel level) {
        FrostysSortingDepot.LOGGER.error("[SD-SELFTEST] FAIL: {}", reason);
        cleanup.forEach(p -> level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
    }
}
