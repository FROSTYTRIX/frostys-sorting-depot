package net.frostytrix.sortingdepot.item;

import java.util.List;

import net.frostytrix.sortingdepot.blockentity.DepotControllerBlockEntity;
import net.frostytrix.sortingdepot.blockentity.LinkerNodeBlockEntity;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The Linker tool, used to wire destinations into a network. Right-click a <b>Linker Node</b> to select
 * it, then right-click a <b>Depot Controller</b> to register the selected node into that Controller's
 * network. Works in either hand.
 */
public class LinkerItem extends Item {

    public LinkerItem(Properties properties) {
        super(properties);
    }

    /** The Linker Node this tool currently has selected, or {@code null} if none. */
    public static @Nullable BlockPos linkedPos(ItemStack stack) {
        return stack.get(SDDataComponents.LINKED_POS.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        BlockEntity be = level.getBlockEntity(clicked);

        if (be instanceof DepotControllerBlockEntity controller) {
            BlockPos selected = linkedPos(stack);
            if (selected != null && level.getBlockEntity(selected) instanceof LinkerNodeBlockEntity node) {
                if (controller.addLinker(selected)) {
                    node.setControllerPos(clicked.immutable());
                    stack.remove(SDDataComponents.LINKED_POS.get());
                    message(player, "item.frostyssortingdepot.linker.registered");
                } else {
                    message(player, "item.frostyssortingdepot.linker.network_full");
                }
            } else {
                message(player, "item.frostyssortingdepot.linker.no_selection");
            }
            return InteractionResult.SUCCESS;
        }

        if (be instanceof LinkerNodeBlockEntity) {
            stack.set(SDDataComponents.LINKED_POS.get(), clicked.immutable());
            message(player, "item.frostyssortingdepot.linker.stored", clicked.getX(), clicked.getY(), clicked.getZ());
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static void message(@Nullable Player player, String key, Object... args) {
        if (player instanceof ServerPlayer serverPlayer) {
            // overlay = true -> action bar, less spammy than chat for tool feedback.
            serverPlayer.sendSystemMessage(Component.translatable(key, args), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos pos = linkedPos(stack);
        Component line = (pos == null)
                ? Component.translatable("item.frostyssortingdepot.linker.unlinked")
                : Component.translatable("item.frostyssortingdepot.linker.linked", pos.getX(), pos.getY(), pos.getZ());
        tooltip.add(line.copy().withStyle(ChatFormatting.GRAY));
    }
}
