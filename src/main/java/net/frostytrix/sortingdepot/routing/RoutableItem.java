package net.frostytrix.sortingdepot.routing;

import java.util.Set;

/**
 * An abstract, Minecraft-free snapshot of an item being routed.
 *
 * <p>This is the only item representation the {@code routing} package understands. Block entities
 * adapt a real {@code ItemStack} into a {@code RoutableItem} before handing it to the
 * {@link RoutingEngine}; the routing layer never touches Minecraft types.
 *
 * @param itemId            registry id of the item, e.g. {@code "minecraft:oak_log"}
 * @param count             stack size of this snapshot (never used for matching, only carried through)
 * @param tags              tag keys this item belongs to (e.g. {@code "minecraft:logs"}), resolved
 *                          externally against the item registry by the adapter — never resolved here
 * @param componentSnapshot opaque string capturing the item's data components for strict matching,
 *                          or {@code null} when component identity is irrelevant
 */
public record RoutableItem(String itemId, int count, Set<String> tags, String componentSnapshot) {

    public RoutableItem {
        if (itemId == null) {
            throw new IllegalArgumentException("itemId must not be null");
        }
        // Defensively copy + null-guard the tag set so callers cannot mutate it after construction.
        tags = (tags == null) ? Set.of() : Set.copyOf(tags);
    }

    /** Convenience factory for the common case of an untagged, component-agnostic item. */
    public static RoutableItem of(String itemId, int count) {
        return new RoutableItem(itemId, count, Set.of(), null);
    }
}
