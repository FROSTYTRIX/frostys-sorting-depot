package net.frostytrix.sortingdepot.item;

import java.util.function.Consumer;

import net.frostytrix.sortingdepot.blockentity.LinkerNodeBlockEntity;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A Priority Stamp. Holds a priority value 1–5 (default 3). Right-click in air cycles up, sneak cycles
 * down (both wrap). Right-click a placed Linker Node to apply this value as that node's priority.
 */
public class PriorityStampItem extends Item {

    public static final int MIN = 1;
    public static final int MAX = 5;
    public static final int DEFAULT = 3;

    public PriorityStampItem(Properties properties) {
        super(properties);
    }

    public static int priority(ItemStack stack) {
        return stack.getOrDefault(SDDataComponents.PRIORITY.get(), DEFAULT);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()
                && level.getBlockEntity(context.getClickedPos()) instanceof LinkerNodeBlockEntity node) {
            int value = priority(context.getItemInHand());
            node.setPriority(value);
            if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("item.frostyssortingdepot.priority_stamp.applied", value), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            int current = priority(stack);
            int next = player.isSecondaryUseActive()
                    ? (current <= MIN ? MAX : current - 1)
                    : (current >= MAX ? MIN : current + 1);
            stack.set(SDDataComponents.PRIORITY.get(), next);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.frostyssortingdepot.priority_stamp.value", priority(stack))
                .withStyle(ChatFormatting.GRAY));
    }
}
