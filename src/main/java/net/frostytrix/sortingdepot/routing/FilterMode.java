package net.frostytrix.sortingdepot.routing;

import java.util.Set;

/**
 * The ways a Filter Card can decide whether it accepts an item.
 *
 * <p>Sealed so {@link FilterMatcher} can switch over it exhaustively with no {@code default} branch —
 * adding a new variant becomes a compile error until the matcher handles it. Pure data, no Minecraft.
 */
public sealed interface FilterMode
        permits FilterMode.ItemFilter, FilterMode.TagFilter, FilterMode.ModFilter, FilterMode.OverflowFilter {

    /**
     * Exact item match against a small set of keys.
     *
     * <p>When {@code strict} is {@code false} the keys are plain registry ids ({@code "minecraft:oak_log"})
     * and an item matches on id alone. When {@code strict} is {@code true} the keys are
     * {@linkplain RoutableItem#strictKey() strict keys} (id + component snapshot), so only items with the
     * same components match — e.g. a specific enchanted book, not every book.
     *
     * @param keys   accepted keys (ids when lenient, strict keys when strict); up to 5 in practice
     * @param strict whether {@code keys} are strict keys and matching compares components
     */
    record ItemFilter(Set<String> keys, boolean strict) implements FilterMode {
        public ItemFilter {
            keys = (keys == null) ? Set.of() : Set.copyOf(keys);
        }

        /** Single-id lenient item filter (matches on id, ignoring components). */
        public static ItemFilter lenient(String itemId) {
            return new ItemFilter(Set.of(itemId), false);
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

    /**
     * Mod match: accepts an item whose registry namespace is one of these, e.g. {@code "create"} catches
     * everything from Create. Namespaces are derived from the items added to the card.
     *
     * @param namespaces accepted namespaces (the part before {@code :} in an item id)
     */
    record ModFilter(Set<String> namespaces) implements FilterMode {
        public ModFilter {
            namespaces = (namespaces == null) ? Set.of() : Set.copyOf(namespaces);
        }
    }

    /** Wildcard: accepts anything. Used by Overflow-mode cards and the Overflow Chest. */
    record OverflowFilter() implements FilterMode {}
}
