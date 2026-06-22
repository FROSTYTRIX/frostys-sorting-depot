package net.frostytrix.sortingdepot.routing;

import java.util.Objects;

/**
 * Pure matching logic: does a given item satisfy a given filter?
 *
 * <p><strong>Zero Minecraft imports.</strong> Tag membership is precomputed onto
 * {@link RoutableItem#tags()} by the caller, so this class never resolves a registry.
 */
public final class FilterMatcher {

    private FilterMatcher() {
    }

    /**
     * @return {@code true} if {@code item} is accepted by {@code filter}.
     */
    public static boolean matches(RoutableItem item, FilterMode filter) {
        return switch (filter) {
            case FilterMode.ItemFilter f -> matchesItem(item, f);
            case FilterMode.TagFilter f -> matchesTag(item, f);
            case FilterMode.OverflowFilter ignored -> true;
        };
    }

    private static boolean matchesItem(RoutableItem item, FilterMode.ItemFilter filter) {
        if (!item.itemId().equals(filter.itemId())) {
            return false;
        }
        // In strict mode the component snapshots must be identical too (e.g. an enchanted book
        // with specific enchantments vs. a plain one).
        return !filter.strict() || Objects.equals(item.componentSnapshot(), filter.componentSnapshot());
    }

    private static boolean matchesTag(RoutableItem item, FilterMode.TagFilter filter) {
        // Accept if the item belongs to any tag the card lists. Iterate the smaller set for cheapness.
        var itemTags = item.tags();
        var cardTags = filter.tagKeys();
        if (itemTags.isEmpty() || cardTags.isEmpty()) {
            return false;
        }
        if (itemTags.size() <= cardTags.size()) {
            for (String t : itemTags) {
                if (cardTags.contains(t)) {
                    return true;
                }
            }
        } else {
            for (String t : cardTags) {
                if (itemTags.contains(t)) {
                    return true;
                }
            }
        }
        return false;
    }
}
