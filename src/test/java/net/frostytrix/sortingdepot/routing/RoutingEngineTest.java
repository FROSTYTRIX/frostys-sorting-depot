package net.frostytrix.sortingdepot.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.frostytrix.sortingdepot.routing.RoutingEngine.Candidate;

class RoutingEngineTest {

    private static RoutableItem oakLog() {
        return new RoutableItem("minecraft:oak_log", 1, Set.of("minecraft:logs"), null);
    }

    private static Candidate accepts(String itemId, int priority, boolean slotAvailable) {
        return new Candidate(FilterMode.ItemFilter.lenient(itemId), priority, slotAvailable);
    }

    @Test
    void singleMatchingAvailableCandidateWins() {
        var candidates = List.of(accepts("minecraft:oak_log", 3, true));
        assertEquals(0, RoutingEngine.chooseTarget(oakLog(), candidates));
    }

    @Test
    void noCandidatesYieldsNoTarget() {
        assertEquals(RoutingEngine.NO_TARGET, RoutingEngine.chooseTarget(oakLog(), List.of()));
    }

    @Test
    void nonMatchingCandidateYieldsNoTarget() {
        var candidates = List.of(accepts("minecraft:stone", 3, true));
        assertEquals(RoutingEngine.NO_TARGET, RoutingEngine.chooseTarget(oakLog(), candidates));
    }

    @Test
    void matchingButFullCandidateIsSkipped() {
        var candidates = List.of(accepts("minecraft:oak_log", 3, false));
        assertEquals(RoutingEngine.NO_TARGET, RoutingEngine.chooseTarget(oakLog(), candidates));
    }

    @Test
    void higherPriorityWinsRegardlessOfListOrder() {
        // Lower priority listed first, higher second — engine must still pick the higher (index 1).
        var candidates = List.of(
                accepts("minecraft:oak_log", 2, true),
                accepts("minecraft:oak_log", 5, true));
        assertEquals(1, RoutingEngine.chooseTarget(oakLog(), candidates));
    }

    @Test
    void equalPriorityBreaksByInsertionOrder() {
        var candidates = List.of(
                accepts("minecraft:oak_log", 3, true),
                accepts("minecraft:oak_log", 3, true));
        assertEquals(0, RoutingEngine.chooseTarget(oakLog(), candidates));
    }

    @Test
    void fullHighPriorityFallsBackToLowerPriorityWithRoom() {
        // Priority-5 chest is full; routing must fall through to the available priority-2 chest.
        var candidates = List.of(
                accepts("minecraft:oak_log", 5, false),
                accepts("minecraft:oak_log", 2, true));
        assertEquals(1, RoutingEngine.chooseTarget(oakLog(), candidates));
    }

    @Test
    void overflowFilterActsAsLastResort() {
        // The exact-match chest rejects the item; the overflow candidate catches it.
        var candidates = List.of(
                accepts("minecraft:stone", 5, true),
                new Candidate(new FilterMode.OverflowFilter(), 1, true));
        assertEquals(1, RoutingEngine.chooseTarget(oakLog(), candidates));
    }

    @Test
    void exactMatchBeatsLowerPriorityOverflow() {
        // Even though overflow accepts everything, the higher-priority exact match should win.
        var candidates = List.of(
                new Candidate(new FilterMode.OverflowFilter(), 1, true),
                accepts("minecraft:oak_log", 4, true));
        assertEquals(1, RoutingEngine.chooseTarget(oakLog(), candidates));
    }

    @Test
    void tagCandidateMatchesViaItemTags() {
        var candidates = List.of(
                new Candidate(new FilterMode.TagFilter(Set.of("minecraft:logs")), 3, true));
        assertEquals(0, RoutingEngine.chooseTarget(oakLog(), candidates));
    }

    @Test
    void roundRobinRotatesAmongEqualPriorityDestinations() {
        var candidates = List.of(
                accepts("minecraft:oak_log", 3, true),
                accepts("minecraft:oak_log", 3, true),
                accepts("minecraft:oak_log", 3, true));
        assertEquals(0, RoutingEngine.chooseRoundRobin(oakLog(), candidates, 0));
        assertEquals(1, RoutingEngine.chooseRoundRobin(oakLog(), candidates, 1));
        assertEquals(2, RoutingEngine.chooseRoundRobin(oakLog(), candidates, 2));
        assertEquals(0, RoutingEngine.chooseRoundRobin(oakLog(), candidates, 3)); // wraps
    }

    @Test
    void roundRobinStaysWithinTheHighestPriorityTier() {
        // Two priority-5 destinations and a priority-2; rotation must never pick the lower tier.
        var candidates = List.of(
                accepts("minecraft:oak_log", 5, true),
                accepts("minecraft:oak_log", 2, true),
                accepts("minecraft:oak_log", 5, true));
        assertEquals(0, RoutingEngine.chooseRoundRobin(oakLog(), candidates, 0));
        assertEquals(2, RoutingEngine.chooseRoundRobin(oakLog(), candidates, 1));
        assertEquals(0, RoutingEngine.chooseRoundRobin(oakLog(), candidates, 2)); // wraps within the 5-tier
    }

    @Test
    void roundRobinSkipsFullDestinations() {
        var candidates = List.of(
                accepts("minecraft:oak_log", 3, false),
                accepts("minecraft:oak_log", 3, true));
        assertEquals(1, RoutingEngine.chooseRoundRobin(oakLog(), candidates, 0));
        assertEquals(1, RoutingEngine.chooseRoundRobin(oakLog(), candidates, 1));
    }

    @Test
    void roundRobinYieldsNoTargetWhenNoneAccept() {
        var candidates = List.of(accepts("minecraft:stone", 3, true));
        assertEquals(RoutingEngine.NO_TARGET, RoutingEngine.chooseRoundRobin(oakLog(), candidates, 0));
    }
}
