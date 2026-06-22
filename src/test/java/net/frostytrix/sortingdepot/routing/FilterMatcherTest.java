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
        void lenientIgnoresComponents() {
            var enchanted = new RoutableItem("minecraft:diamond_sword", 1, Set.of(), "{sharpness:5}");
            assertTrue(FilterMatcher.matches(enchanted, FilterMode.ItemFilter.lenient("minecraft:diamond_sword")));
        }

        @Test
        void strictMatchesWhenComponentsEqual() {
            var item = new RoutableItem("minecraft:diamond_sword", 1, Set.of(), "{sharpness:5}");
            var filter = new FilterMode.ItemFilter("minecraft:diamond_sword", "{sharpness:5}", true);
            assertTrue(FilterMatcher.matches(item, filter));
        }

        @Test
        void strictRejectsWhenComponentsDiffer() {
            var item = new RoutableItem("minecraft:diamond_sword", 1, Set.of(), "{sharpness:1}");
            var filter = new FilterMode.ItemFilter("minecraft:diamond_sword", "{sharpness:5}", true);
            assertFalse(FilterMatcher.matches(item, filter));
        }

        @Test
        void strictMatchesWhenBothComponentsNull() {
            var item = new RoutableItem("minecraft:stick", 1, Set.of(), null);
            var filter = new FilterMode.ItemFilter("minecraft:stick", null, true);
            assertTrue(FilterMatcher.matches(item, filter));
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
}
