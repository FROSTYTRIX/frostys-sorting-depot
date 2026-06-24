package net.frostytrix.sortingdepot.routing;

import java.util.Set;

/**
 * The three ways a Filter Card can decide whether it accepts an item.
 *
 * <p>Sealed so {@link FilterMatcher} can switch over it exhaustively with no {@code default} branch —
 * adding a new variant becomes a compile error until the matcher handles it. Pure data, no Minecraft.
 */
public sealed interface FilterMode
        permits FilterMode.ItemFilter, FilterMode.TagFilter, FilterMode.OverflowFilter {

    /**
     * Exact item match against a small set of registry ids. The card accepts an item if its id is one of
     * {@code itemIds}. Components are ignored (an item matches on id alone) — this keeps multi-item cards
     * simple, which is the only way item cards are configured from the GUI.
     *
     * @param itemIds registry ids this card accepts, e.g. {@code "minecraft:oak_log"} (up to 5 in practice)
     */
    record ItemFilter(Set<String> itemIds) implements FilterMode {
        public ItemFilter {
            itemIds = (itemIds == null) ? Set.of() : Set.copyOf(itemIds);
        }

        /** Single-id item filter. */
        public static ItemFilter lenient(String itemId) {
            return new ItemFilter(Set.of(itemId));
        }
    }

    /**
     * Tag match: accepts an item if it belongs to <em>any</em> of these tag keys. Membership is carried
     * on {@link RoutableItem#tags()} (resolved externally), so matching here is a pure set intersection.
     *
     * @param tagKeys tag keys this card accepts, e.g. {@code "minecraft:logs"}
     */
    record TagFilter(Set<String> tagKeys) implements FilterMode {
        public TagFilter {
            tagKeys = (tagKeys == null) ? Set.of() : Set.copyOf(tagKeys);
        }
    }

    /** Wildcard: accepts anything. Used by Overflow-mode cards and the Overflow Chest. */
    record OverflowFilter() implements FilterMode {}
}
