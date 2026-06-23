package net.frostytrix.sortingdepot.item;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A Filter Card. Stores a {@link FilterCardData} component describing what it accepts. Sneak-use cycles
 * the matching mode (Item → Tag → Overflow); right-clicking the card onto an item stack configures the
 * target (see {@link #overrideStackedOnOther}). The tooltip reflects the current configuration.
 */
public class FilterCardItem extends Item {

    public FilterCardItem(Properties properties) {
        super(properties);
    }

    /** Reads the card's config, defaulting to {@link FilterCardData#EMPTY} when absent. */
    public static FilterCardData data(ItemStack stack) {
        return stack.getOrDefault(SDDataComponents.FILTER_DATA.get(), FilterCardData.EMPTY);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide()) {
                FilterCardData current = data(stack);
                stack.set(SDDataComponents.FILTER_DATA.get(), current.withMode(current.nextMode()));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Right-click this card (on the cursor) onto an item stack in a GUI to configure it, mode-aware:
     * <ul>
     *   <li><b>Item</b> mode → binds to that exact item.</li>
     *   <li><b>Tag</b> mode → binds to one of the item's tags, cycling through them on repeated clicks
     *       (watch the tooltip).</li>
     *   <li><b>Overflow</b> mode → nothing to bind.</li>
     * </ul>
     * Returns {@code true} to consume the click instead of swapping the stacks.
     */
    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot other, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack target = other.getItem();
        if (target.isEmpty() || target.getItem() instanceof FilterCardItem) {
            return false;
        }
        FilterCardData data = data(stack);
        return switch (data.mode()) {
            case ITEM -> {
                Identifier id = BuiltInRegistries.ITEM.getKey(target.getItem());
                stack.set(SDDataComponents.FILTER_DATA.get(), FilterCardData.ofItem(id, false));
                yield true;
            }
            case TAG -> bindNextTag(stack, data, target);
            case OVERFLOW -> false;
        };
    }

    /** Binds the card to the next tag of {@code target}, cycling through them on repeated clicks. */
    private static boolean bindNextTag(ItemStack stack, FilterCardData data, ItemStack target) {
        List<Identifier> itemTags = target.typeHolder().tags()
                .map(TagKey::location)
                .sorted(Comparator.comparing(Identifier::toString))
                .collect(Collectors.toList());
        if (itemTags.isEmpty()) {
            return false; // nothing to bind to
        }
        Identifier current = data.tags().stream().findFirst().orElse(null);
        int index = current == null ? -1 : itemTags.indexOf(current);
        Identifier next = itemTags.get((index + 1) % itemTags.size());
        stack.set(SDDataComponents.FILTER_DATA.get(),
                new FilterCardData(FilterCardData.Mode.TAG, Optional.empty(), Set.of(next), false));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        FilterCardData d = data(stack);
        builder.accept(Component.translatable("item.frostyssortingdepot.filter_card.mode",
                        Component.translatable("item.frostyssortingdepot.filter_card.mode." + d.mode().getSerializedName()))
                .withStyle(ChatFormatting.GRAY));

        Component detail = switch (d.mode()) {
            case ITEM -> Component.translatable("item.frostyssortingdepot.filter_card.target",
                    d.itemId().map(Identifier::toString).orElse("—"));
            case TAG -> Component.translatable("item.frostyssortingdepot.filter_card.tags",
                    d.tags().stream().findFirst().map(id -> "#" + id).orElse("—"));
            case OVERFLOW -> Component.translatable("item.frostyssortingdepot.filter_card.overflow");
        };
        builder.accept(detail.copy().withStyle(ChatFormatting.DARK_GRAY));
    }
}
