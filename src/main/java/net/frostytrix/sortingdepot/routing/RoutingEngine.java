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

    /**
     * Like {@link #chooseTarget} but, among the eligible destinations sharing the <em>highest</em>
     * priority, distributes in rotation instead of always picking the first. {@code rotation} is a
     * monotonically increasing counter the caller advances after each successful move; the winner is
     * {@code tier[rotation mod tier.size]}. Lower priorities are still only used when the whole top tier
     * is full/unmatched, so priority ordering is preserved — round-robin only load-balances ties.
     *
     * @return the chosen index, or {@link #NO_TARGET} if no candidate accepts the item
     */
    public static int chooseRoundRobin(RoutableItem item, List<Candidate> candidates, int rotation) {
        int bestPriority = Integer.MIN_VALUE;
        int tierSize = 0;
        // First pass: find the highest priority among eligible candidates and how many share it.
        for (Candidate c : candidates) {
            if (!c.slotAvailable() || !FilterMatcher.matches(item, c.filter())) {
                continue;
            }
            if (c.priority() > bestPriority) {
                bestPriority = c.priority();
                tierSize = 1;
            } else if (c.priority() == bestPriority) {
                tierSize++;
            }
        }
        if (tierSize == 0) {
            return NO_TARGET;
        }
        // Second pass: walk the tier in list order and return the one at the rotated position.
        int pick = Math.floorMod(rotation, tierSize);
        int seen = 0;
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            if (!c.slotAvailable() || !FilterMatcher.matches(item, c.filter()) || c.priority() != bestPriority) {
                continue;
            }
            if (seen == pick) {
                return i;
            }
            seen++;
        }
        return NO_TARGET; // unreachable
    }
}
