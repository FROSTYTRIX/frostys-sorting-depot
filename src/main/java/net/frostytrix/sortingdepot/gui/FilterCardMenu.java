package net.frostytrix.sortingdepot.gui;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.frostytrix.sortingdepot.item.FilterCardItem;
import net.frostytrix.sortingdepot.item.component.FilterCardData;
import net.frostytrix.sortingdepot.registry.SDDataComponents;
import net.frostytrix.sortingdepot.registry.SDMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Configuration menu for a held Filter Card. The card stays in the player's hand (it is not a slot here);
 * the menu shows only the player inventory so the player can click items to add them to the filter. All
 * edits flow through {@link #clickMenuButton} so no custom networking is needed.
 *
 * <p>The card holds up to {@link FilterCardData#MAX_ITEMS} items and {@link FilterCardData#MAX_TAGS} tags.
 * The active {@link FilterCardData.Mode} decides which is used when routing; both are retained so the
 * pickers can be rebuilt from the chosen items.
 */
public class FilterCardMenu extends AbstractContainerMenu {

    // Button protocol (driven from FilterCardScreen via clickMenuButton):
    public static final int BTN_MODE_ITEM = 0;
    public static final int BTN_MODE_MOD = 1;
    public static final int BTN_MODE_TAG = 2;
    public static final int BTN_MODE_OVERFLOW = 3;
    /** Toggle strict (component-aware) matching for Item mode. */
    public static final int BTN_TOGGLE_STRICT = 4;
    /** Toggle the NOT/exclude wrapper — inverts whichever mode the card is in. */
    public static final int BTN_TOGGLE_NEGATED = 5;
    /** Remove the item at index {@code id - BTN_REMOVE_ITEM} (0..MAX_ITEMS-1). */
    public static final int BTN_REMOVE_ITEM = 100;
    /** Toggle the tag at index {@code id - BTN_TOGGLE_TAG} within {@link #displayedTags}. */
    public static final int BTN_TOGGLE_TAG = 200;
    /** Add the item in player-inventory menu-slot {@code id - BTN_ADD_SLOT} to the item list. */
    public static final int BTN_ADD_SLOT = 1000;

    private final Player player;
    private final InteractionHand hand;

    /** Client-side constructor: reads which hand holds the card. */
    public FilterCardMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, data.readEnum(InteractionHand.class));
    }

    /** Server-side constructor. */
    public FilterCardMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(SDMenus.FILTER_CARD.get(), containerId);
        this.player = playerInventory.player;
        this.hand = hand;

        // Player inventory (3 rows) then hotbar. Slot indices 0..35 — the screen targets these for "add".
        // Coordinates are the item origin (1px inside the frames FilterCardScreen draws); keep them in sync.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 9 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 9 + col * 18, 198));
        }
    }

    /** The card currently held in the configured hand (may not be a Filter Card if the player swapped). */
    public ItemStack getCard() {
        return player.getItemInHand(hand);
    }

    /** The card's current configuration, or {@link FilterCardData#EMPTY} if it is no longer a card. */
    public FilterCardData filterData() {
        ItemStack card = getCard();
        return card.getItem() instanceof FilterCardItem ? FilterCardItem.data(card) : FilterCardData.EMPTY;
    }

    /**
     * The tags offered for the current item list: the de-duplicated union of every listed item's tags,
     * sorted by id. Computed identically on client (to render the checklist) and server (to resolve a
     * toggle-by-index click) so the two never disagree.
     */
    public static List<Identifier> availableTags(List<ItemStack> items) {
        LinkedHashSet<Identifier> tags = new LinkedHashSet<>();
        for (ItemStack stack : items) {
            stack.typeHolder().tags().map(TagKey::location).forEach(tags::add);
        }
        return tags.stream().sorted(Comparator.comparing(Identifier::toString)).toList();
    }

    /**
     * The tags shown in the checklist: the union of {@link #availableTags} and any already-{@code selected}
     * tags, sorted by id. Including selected tags guarantees a tag stays visible (and so removable) even
     * after the item that contributed it is removed — otherwise it would be stuck on the card.
     */
    public static List<Identifier> displayedTags(List<ItemStack> items, Set<Identifier> selected) {
        LinkedHashSet<Identifier> tags = new LinkedHashSet<>(availableTags(items));
        tags.addAll(selected);
        return tags.stream().sorted(Comparator.comparing(Identifier::toString)).toList();
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        ItemStack card = getCard();
        if (!(card.getItem() instanceof FilterCardItem)) {
            return false;
        }
        FilterCardData d = FilterCardItem.data(card);
        FilterCardData updated;
        if (id == BTN_MODE_ITEM) {
            updated = d.withMode(FilterCardData.Mode.ITEM);
        } else if (id == BTN_MODE_MOD) {
            updated = d.withMode(FilterCardData.Mode.MOD);
        } else if (id == BTN_MODE_TAG) {
            updated = d.withMode(FilterCardData.Mode.TAG);
        } else if (id == BTN_MODE_OVERFLOW) {
            updated = d.withMode(FilterCardData.Mode.OVERFLOW);
        } else if (id == BTN_TOGGLE_STRICT) {
            updated = d.withStrictToggled();
        } else if (id == BTN_TOGGLE_NEGATED) {
            updated = d.withNegatedToggled();
        } else if (id >= BTN_REMOVE_ITEM && id < BTN_REMOVE_ITEM + FilterCardData.MAX_ITEMS) {
            updated = d.withItemRemovedAt(id - BTN_REMOVE_ITEM);
        } else if (id >= BTN_ADD_SLOT) {
            int slotIndex = id - BTN_ADD_SLOT;
            if (slotIndex < 0 || slotIndex >= slots.size()) {
                return false;
            }
            ItemStack toAdd = slots.get(slotIndex).getItem();
            if (toAdd.isEmpty() || toAdd.getItem() instanceof FilterCardItem) {
                return false;
            }
            updated = d.withItemAdded(toAdd);
        } else if (id >= BTN_TOGGLE_TAG) {
            List<Identifier> tags = displayedTags(d.items(), d.tags());
            int idx = id - BTN_TOGGLE_TAG;
            if (idx < 0 || idx >= tags.size()) {
                return false;
            }
            updated = d.withTagToggled(tags.get(idx));
        } else {
            return false;
        }

        // The with* helpers return the same instance when nothing changed, so identity is the cheap check
        // (and avoids ItemStack's reference-based equals inside the record).
        if (updated != d) {
            card.set(SDDataComponents.FILTER_DATA.get(), updated);
        }
        return true;
    }

    @Override
    public boolean stillValid(Player p) {
        return getCard().getItem() instanceof FilterCardItem;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        // No transfer target in this config menu; shift-click is a no-op.
        return ItemStack.EMPTY;
    }
}
