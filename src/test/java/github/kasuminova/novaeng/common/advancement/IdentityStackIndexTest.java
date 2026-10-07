package github.kasuminova.novaeng.common.advancement;

import org.junit.jupiter.api.Test;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class IdentityStackIndexTest {
    private static final BiPredicate<Predicate<Stack>, Stack> TEST = Predicate::test;

    @Test
    void preservesIndividualStackCountsAndTags() {
        final IdentityStackIndex<Object, Stack> index = new IdentityStackIndex<>();
        final Object item = new Object();
        index.add(item, new Stack(4, "a"));
        index.add(item, new Stack(4, "b"));
        assertFalse(index.matches(item, s -> s.count >= 8, TEST));
        assertTrue(index.matches(item, s -> s.tag.equals("b"), TEST));
        assertFalse(index.matches(item, s -> s.tag.equals("c"), TEST));
        assertTrue(index.matches(item, s -> s.count == 4, TEST));
        assertTrue(index.matches(item, s -> s.count == 4, TEST));
    }

    @Test
    void namedMissDoesNotEvaluateAndWildcardVisitsInSlotOrder() {
        final IdentityStackIndex<Object, Stack> index = new IdentityStackIndex<>();
        final Object a = new String("same");
        final Object b = new String("same");
        index.add(a, new Stack(1, "first"));
        index.add(a, new Stack(2, "last"));
        assertFalse(index.matches(b, s -> {
            fail("identity miss must not visit a stack");
            return true;
        }, TEST));
        final int[] expected = {1};
        assertFalse(index.matches(null, s -> {
            assertEquals(expected[0]++, s.count);
            return false;
        }, TEST));
        assertEquals(3, expected[0]);
    }

    @Test
    void rebuildingDoesNotRetainOldItemsOrStacks() {
        final IdentityStackIndex<Object, Stack> index = new IdentityStackIndex<>();
        final Object old = new Object();
        for (int i = 0; i < 200; i++) {
            index.add(old, new Stack(i, "old"));
        }
        index.clear();
        assertFalse(index.contains(old));
        assertEquals(0, index.size());
        final Object current = new Object();
        index.add(current, new Stack(7, "new"));
        assertTrue(index.matches(current, s -> s.count == 7, TEST));
        assertFalse(index.matches(old, s -> true, TEST));
    }

    private record Stack(int count, String tag) {
    }
}
