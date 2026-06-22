package net.frostytrix.sortingdepot.item;

import java.util.function.Consumer;

import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The Linker tool. Right-clicking a block stores that block's position on the item; a later use on a
 * Depot Controller registers the stored position into the Controller's network. Works in either hand.
 */
public class LinkerItem extends Item {

    public LinkerItem(Properties properties) {
        super(properties);
    }

    /** The position this Linker currently points at, or {@code null} if unlinked. */
    public static BlockPos linkedPos(ItemStack stack) {
        return stack.get(SDDataComponents.LINKED_POS.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide()) {
            // Store the targeted block's position. Controller registration is handled in a later phase.
            context.getItemInHand().set(SDDataComponents.LINKED_POS.get(), context.getClickedPos().immutable());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        BlockPos pos = linkedPos(stack);
        Component line = (pos == null)
                ? Component.translatable("item.frostyssortingdepot.linker.unlinked")
                : Component.translatable("item.frostyssortingdepot.linker.linked", pos.getX(), pos.getY(), pos.getZ());
        builder.accept(line.copy().withStyle(ChatFormatting.GRAY));
    }
}
