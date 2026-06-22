package net.frostytrix.sortingdepot.routing;

import java.util.List;

/**
 * Pure routing decision: given an item and an ordered list of candidate destinations, pick the winner.
 *
 * <p><strong>Zero Minecraft imports.</strong> The block entity builds the candidate list from its
 * registered Linkers (each already validated and capacity-checked) and acts on the returned index.
 */
public final class RoutingEngine {

    private RoutingEngine() {
    }

    /**
     * A single destination the engine may choose.
     *
     * @param filter        the destination's filter mode
     * @param priority      1–5, higher is tried first
     * @param slotAvailable whether the destination currently has room for the item
     */
    public record Candidate(FilterMode filter, int priority, boolean slotAvailable) {
    }

    /** Sentinel returned when no candidate accepts the item. */
    public static final int NO_TARGET = -1;

    /**
     * Choose the destination index for {@code item}.
     *
     * <p>A candidate is eligible only if it has room ({@code slotAvailable}) and its filter
     * {@linkplain FilterMatcher#matches accepts} the item. Among eligible candidates the highest
     * {@code priority} wins; ties are broken by insertion order (earliest registered wins), matching
     * the order the list is passed in.
     *
     * @return the index into {@code candidates} of the winner, or {@link #NO_TARGET} if none match
     */
    public static int chooseTarget(RoutableItem item, List<Candidate> candidates) {
        int bestIndex = NO_TARGET;
        int bestPriority = Integer.MIN_VALUE;
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            if (!c.slotAvailable() || !FilterMatcher.matches(item, c.filter())) {
                continue;
            }
            // Strictly-greater keeps the first candidate seen at the top priority, so equal
            // priorities resolve by insertion order automatically.
            if (c.priority() > bestPriority) {
                bestPriority = c.priority();
                bestIndex = i;
            }
        }
        return bestIndex;
    }
}
