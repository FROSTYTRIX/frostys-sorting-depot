package net.frostytrix.sortingdepot.routing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FilterMatcherTest {

    private static RoutableItem oakLog() {
        return new RoutableItem("minecraft:oak_log", 1, Set.of("minecraft:logs", "minecraft:oak_logs"), null);
    }

    @Nested
    @DisplayName("ItemFilter")
    class ItemMode {

        @Test
        void lenientMatchesSameId() {
            assertTrue(FilterMatcher.matches(oakLog(), FilterMode.ItemFilter.lenient("minecraft:oak_log")));
        }

        @Test
        void lenientRejectsDifferentId() {
            assertFalse(FilterMatcher.matches(oakLog(), FilterMode.ItemFilter.lenient("minecraft:stone")));
        }

        @Test
        void matchesIgnoringComponents() {
            var enchanted = new RoutableItem("minecraft:diamond_sword", 1, Set.of(), "{sharpness:5}");
            assertTrue(FilterMatcher.matches(enchanted, FilterMode.ItemFilter.lenient("minecraft:diamond_sword")));
        }

        @Test
        void matchesWhenIdIsOneOfSeveral() {
            var filter = new FilterMode.ItemFilter(Set.of("minecraft:oak_log", "minecraft:birch_log"), false);
            assertTrue(FilterMatcher.matches(oakLog(), filter));
        }

        @Test
        void rejectsWhenIdNotInSet() {
            var filter = new FilterMode.ItemFilter(Set.of("minecraft:birch_log", "minecraft:spruce_log"), false);
            assertFalse(FilterMatcher.matches(oakLog(), filter));
        }

        @Test
        void emptySetMatchesNothing() {
            assertFalse(FilterMatcher.matches(oakLog(), new FilterMode.ItemFilter(Set.of(), false)));
        }

        @Test
        void strictMatchesOnlyWhenComponentsMatch() {
            var sharp5 = new RoutableItem("minecraft:diamond_sword", 1, Set.of(), "{sharpness:5}");
            var plain = new RoutableItem("minecraft:diamond_sword", 1, Set.of(), null);
            var filter = new FilterMode.ItemFilter(Set.of(sharp5.strictKey()), true);
            assertTrue(FilterMatcher.matches(sharp5, filter));
            assertFalse(FilterMatcher.matches(plain, filter)); // same item, different components
        }
    }

    @Nested
    @DisplayName("ModFilter")
    class ModMode {

        @Test
        void matchesItemsInTheNamespace() {
            var create = new RoutableItem("create:cogwheel", 1, Set.of(), null);
            assertTrue(FilterMatcher.matches(create, new FilterMode.ModFilter(Set.of("create"))));
        }

        @Test
        void rejectsItemsFromOtherNamespaces() {
            assertFalse(FilterMatcher.matches(oakLog(), new FilterMode.ModFilter(Set.of("create"))));
        }

        @Test
        void matchesAnyOfSeveralNamespaces() {
            assertTrue(FilterMatcher.matches(oakLog(), new FilterMode.ModFilter(Set.of("create", "minecraft"))));
        }
    }

    @Nested
    @DisplayName("TagFilter")
    class TagMode {

        @Test
        void matchesWhenItemHasOneOfTheTags() {
            assertTrue(FilterMatcher.matches(oakLog(), new FilterMode.TagFilter(Set.of("minecraft:logs"))));
        }

        @Test
        void rejectsWhenNoTagOverlaps() {
            assertFalse(FilterMatcher.matches(oakLog(), new FilterMode.TagFilter(Set.of("minecraft:planks"))));
        }

        @Test
        void rejectsWhenItemHasNoTags() {
            var untagged = new RoutableItem("minecraft:stone", 1, Set.of(), null);
            assertFalse(FilterMatcher.matches(untagged, new FilterMode.TagFilter(Set.of("minecraft:logs"))));
        }

        @Test
        void rejectsWhenCardHasNoTags() {
            assertFalse(FilterMatcher.matches(oakLog(), new FilterMode.TagFilter(Set.of())));
        }
    }

    @Nested
    @DisplayName("OverflowFilter")
    class OverflowMode {

        @Test
        void matchesAnything() {
            assertTrue(FilterMatcher.matches(oakLog(), new FilterMode.OverflowFilter()));
            var weird = new RoutableItem("somemod:strange_item", 64, Set.of(), "{data:1}");
            assertTrue(FilterMatcher.matches(weird, new FilterMode.OverflowFilter()));
        }
    }

    @Nested
    @DisplayName("Negated")
    class NegatedMode {

        @Test
        void invertsItemFilter() {
            var filter = new FilterMode.Negated(FilterMode.ItemFilter.lenient("minecraft:oak_log"));
            assertFalse(FilterMatcher.matches(oakLog(), filter));
            assertTrue(FilterMatcher.matches(
                    new RoutableItem("minecraft:stone", 1, Set.of(), null), filter));
        }

        @Test
        void invertsTagFilter() {
            var filter = new FilterMode.Negated(new FilterMode.TagFilter(Set.of("minecraft:logs")));
            assertFalse(FilterMatcher.matches(oakLog(), filter));
            assertTrue(FilterMatcher.matches(
                    new RoutableItem("minecraft:stone", 1, Set.of(), null), filter));
        }

        @Test
        void invertsModFilter() {
            var filter = new FilterMode.Negated(new FilterMode.ModFilter(Set.of("create")));
            var createGear = new RoutableItem("create:cogwheel", 1, Set.of(), null);
            var vanillaStone = new RoutableItem("minecraft:stone", 1, Set.of(), null);
            assertFalse(FilterMatcher.matches(createGear, filter));
            assertTrue(FilterMatcher.matches(vanillaStone, filter));
        }

        @Test
        void invertsOverflowToRejectEverything() {
            var filter = new FilterMode.Negated(new FilterMode.OverflowFilter());
            assertFalse(FilterMatcher.matches(oakLog(), filter));
            assertFalse(FilterMatcher.matches(
                    new RoutableItem("minecraft:stone", 1, Set.of(), null), filter));
        }

        @Test
        void doubleNegationIsIdentity() {
            var inner = FilterMode.ItemFilter.lenient("minecraft:oak_log");
            var doubled = new FilterMode.Negated(new FilterMode.Negated(inner));
            assertTrue(FilterMatcher.matches(oakLog(), doubled));
            assertFalse(FilterMatcher.matches(
                    new RoutableItem("minecraft:stone", 1, Set.of(), null), doubled));
        }
    }
}
