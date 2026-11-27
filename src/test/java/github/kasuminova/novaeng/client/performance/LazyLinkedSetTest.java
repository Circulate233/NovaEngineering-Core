package github.kasuminova.novaeng.client.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LazyLinkedSetTest {
    @Test
    void matchesInsertionOrderNullDuplicatesAndAllMutations() {
        final Set<String> expected = new ObjectLinkedOpenHashSet<>();
        final Set<String> actual = new LazyLinkedSet<>();
        for (final String value : Arrays.asList("b", null, "a", "b", "c")) {
            assertEquals(expected.add(value), actual.add(value));
            assertState(expected, actual);
        }
        assertEquals(expected.remove(null), actual.remove(null));
        assertState(expected, actual);
        final Iterator<String> first = expected.iterator();
        final Iterator<String> second = actual.iterator();
        assertEquals(first.next(), second.next());
        first.remove();
        second.remove();
        assertState(expected, actual);
        assertEquals(expected.removeAll(List.of("absent", "a")), actual.removeAll(List.of("absent", "a")));
        assertState(expected, actual);
        assertEquals(expected.retainAll(List.of("absent")), actual.retainAll(List.of("absent")));
        assertState(expected, actual);
        actual.add("again");
        expected.add("again");
        assertEquals(expected.removeIf(value -> true), actual.removeIf(value -> true));
        assertState(expected, actual);
        assertThrows(NullPointerException.class, () -> actual.removeIf(null));
        actual.clear();
        assertFalse(actual.iterator().hasNext());
    }

    @Test
    void emptyIteratorsRetainRemovalAndConcurrentModificationBehavior() {
        final LazyLinkedSet<String> set = new LazyLinkedSet<>();
        final Iterator<String> empty = set.iterator();
        assertFalse(empty.hasNext());
        assertThrows(IllegalStateException.class, empty::remove);
        assertThrows(NoSuchElementException.class, empty::next);
        set.add("now present");
        assertThrows(ConcurrentModificationException.class, empty::next);
        final Iterator<String> populated = set.iterator();
        set.add("another");
        assertThrows(ConcurrentModificationException.class, populated::next);
    }

    @Test
    void spliteratorIsOrderedLateBoundAndSerializationOwnsItsState() throws Exception {
        final LazyLinkedSet<String> set = new LazyLinkedSet<>();
        final Spliterator<String> iterator = set.spliterator();
        assertTrue(iterator.hasCharacteristics(Spliterator.ORDERED | Spliterator.DISTINCT));
        set.add("b");
        set.add(null);
        set.add("a");
        final List<String> copied = new ObjectArrayList<>();
        iterator.forEachRemaining(copied::add);
        assertEquals(Arrays.asList("b", null, "a"), copied);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (final ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(set); }
        final Set<?> roundTrip;
        try (final ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            roundTrip = (Set<?>) input.readObject();
        }
        assertState(set, roundTrip);
        set.clear();
        assertEquals(3, roundTrip.size());
    }

    private static void assertState(final Set<?> expected, final Set<?> actual) {
        assertEquals(expected, actual);
        assertEquals(actual, expected);
        assertEquals(expected.hashCode(), actual.hashCode());
        assertArrayEquals(expected.toArray(), actual.toArray());
    }
}
