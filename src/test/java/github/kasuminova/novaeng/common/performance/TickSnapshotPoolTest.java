package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickSnapshotPoolTest {

    private static Object[] array(final Iterator<?> iterator) throws Exception {
        final Field field = iterator.getClass().getDeclaredField("array");
        field.setAccessible(true);
        return (Object[]) field.get(iterator);
    }

    private static <T> List<T> drain(final Iterator<T> iterator) {
        final List<T> values = new ArrayList<>();
        iterator.forEachRemaining(values::add);
        return values;
    }

    @Test
    void selectsEagerlyAndKeepsEncounterOrderAndNullValues() {
        final TickSnapshotPool<Integer> pool = new TickSnapshotPool<>();
        final List<Integer> source = new ArrayList<>(List.of(1, 2, 3, 4));
        final AtomicInteger calls = new AtomicInteger();
        final Iterator<Integer> snapshot = pool.select(source, value -> {
            calls.incrementAndGet();
            return value % 2 == 0;
        }, value -> value == 2 ? null : value);
        assertEquals(4, calls.get());
        source.clear();
        assertEquals(Arrays.asList(null, 4), drain(snapshot));
        assertThrows(NoSuchElementException.class, snapshot::next);
    }

    @Test
    void outstandingAndReentrantSnapshotsHaveIndependentStorage() {
        final TickSnapshotPool<Integer> pool = new TickSnapshotPool<>();
        final Iterator<Integer> first = pool.select(List.of(1, 2, 3), value -> true, value -> value);
        final Iterator<Integer> second = pool.select(List.of(4, 5), value -> true, value -> value);
        assertEquals(1, first.next());
        assertEquals(List.of(4, 5), drain(second));
        final Iterator<Integer> third = pool.select(List.of(6, 7), value -> true, value -> value);
        assertEquals(List.of(2, 3), drain(first));
        assertEquals(List.of(6, 7), drain(third));
        final List<Integer> nested = new ArrayList<>();
        final Iterator<Integer> outer = pool.select(List.of(10, 20), value -> {
            nested.addAll(drain(pool.select(List.of(value + 1), v -> true, v -> v)));
            return true;
        }, value -> value);
        assertEquals(List.of(11, 21), nested);
        assertEquals(List.of(10, 20), drain(outer));
    }

    @Test
    void reusesClearedArraysWithoutRetainingCompletedValues() throws Exception {
        final TickSnapshotPool<Object> pool = new TickSnapshotPool<>();
        final Object value = new Object();
        final Iterator<Object> first = pool.select(List.of(value), v -> true, v -> v);
        final Object[] array = array(first);
        assertSame(value, first.next());
        assertNull(array[0]);
        assertNull(array(first));
        final Iterator<Object> second = pool.select(List.of(new Object()), v -> true, v -> v);
        assertSame(array, array(second));
        drain(second);
        assertFalse(first.hasNext());
        assertThrows(NoSuchElementException.class, first::next);
    }

    @Test
    void iteratorRemoveOnlyChangesItsPrivateSnapshot() {
        final TickSnapshotPool<Integer> pool = new TickSnapshotPool<>();
        final List<Integer> source = new ArrayList<>(List.of(1, 2, 3));
        final Iterator<Integer> iterator = pool.select(source, value -> true, value -> value);
        assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals(1, iterator.next());
        iterator.remove();
        assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals(2, iterator.next());
        assertEquals(3, iterator.next());
        assertFalse(iterator.hasNext());
        iterator.remove();
        assertThrows(IllegalStateException.class, iterator::remove);
        assertEquals(List.of(1, 2, 3), source);
    }

    @Test
    void failuresReturnCleanStorageAndIdlePoolIsBounded() throws Exception {
        final TickSnapshotPool<Integer> pool = new TickSnapshotPool<>();
        assertThrows(IllegalArgumentException.class, () -> pool.select(List.of(1, 2), value -> {
            if (value == 2) {
                throw new IllegalArgumentException();
            }
            return true;
        }, value -> value));
        final List<Iterator<Integer>> snapshots = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            snapshots.add(pool.select(List.of(i), value -> true, value -> value));
        }
        for (final Iterator<Integer> iterator : snapshots) {
            drain(iterator);
        }
        final Field idleField = TickSnapshotPool.class.getDeclaredField("idle");
        idleField.setAccessible(true);
        final ObjectArrayList<?> idle = (ObjectArrayList<?>) idleField.get(pool);
        assertEquals(2, idle.size());
        for (final Object element : idle) {
            assertTrue(Arrays.stream((Object[]) element).allMatch(value -> value == null));
        }
    }

    @Test
    void oversizedAndAbandonedSnapshotsAreNotKeptInIdlePool() throws Exception {
        final TickSnapshotPool<Integer> pool = new TickSnapshotPool<>();
        final Iterator<Integer> abandoned = pool.select(List.of(1, 2), value -> true, value -> value);
        final List<Integer> large = new ArrayList<>();
        for (int i = 0; i < 17000; i++) {
            large.add(i);
        }
        drain(pool.select(large, value -> true, value -> value));
        final Field idleField = TickSnapshotPool.class.getDeclaredField("idle");
        idleField.setAccessible(true);
        assertTrue(((ObjectArrayList<?>) idleField.get(pool)).isEmpty());
        assertEquals(List.of(1, 2), drain(abandoned));
    }
}
