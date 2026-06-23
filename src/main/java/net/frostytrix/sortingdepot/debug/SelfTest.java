package net.frostytrix.sortingdepot.debug;

import java.util.List;

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
        List<BlockPos> all = List.of(chestPos, linkerPos, controllerPos);

        all.forEach(p -> level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));

        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        // ...and faces EAST, toward the chest, so its target inventory resolves to the chest.
        level.setBlockAndUpdate(linkerPos,
                SDBlocks.LINKER_NODE.get().defaultBlockState().setValue(LinkerNodeBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(controllerPos, SDBlocks.DEPOT_CONTROLLER.get().defaultBlockState());

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

        // Feed the Controller and drive routing.
        controller.getInputHandler().set(0, ItemResource.of(new ItemStack(Items.COBBLESTONE)), TEST_COUNT);
        BlockState controllerState = level.getBlockState(controllerPos);
        for (int i = 0; i < 20; i++) {
            DepotControllerBlockEntity.serverTick(level, controllerPos, controllerState, controller);
        }

        int inChest = countInChest(level, chestPos);
        int leftInInput = ItemUtil.getStack(controller.getInputHandler(), 0).getCount();

        if (inChest == TEST_COUNT && leftInInput == 0) {
            FrostysSortingDepot.LOGGER.info(
                    "[SD-SELFTEST] PASS: routed {} cobblestone Controller -> chest via Linker Node", TEST_COUNT);
        } else {
            fail("expected chest=" + TEST_COUNT + " input=0, got chest=" + inChest + " input=" + leftInInput, all, level);
        }

        all.forEach(p -> level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState()));
    }

    private static int countInChest(ServerLevel level, BlockPos chestPos) {
        ResourceHandler<ItemResource> chest = level.getCapability(Capabilities.Item.BLOCK, chestPos, null);
        if (chest == null) {
            return -1;
        }
        int total = 0;
        for (int slot = 0; slot < chest.size(); slot++) {
            ItemStack stack = ItemUtil.getStack(chest, slot);
            if (stack.getItem() == Items.COBBLESTONE) {
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
