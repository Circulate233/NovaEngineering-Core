package github.kasuminova.novaeng.common.util.collection;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeakObject2IntCacheTest {
    @SuppressWarnings("unchecked")
    private static ObjectOpenCustomHashSet<Object> map(final WeakObject2IntCache<?> cache) throws Exception {
        final Field field = WeakObject2IntCache.class.getDeclaredField("values");
        field.setAccessible(true);
        return (ObjectOpenCustomHashSet<Object>) field.get(cache);
    }

    private static int capacity(final ObjectOpenCustomHashSet<?> map) throws Exception {
        final Field field = ObjectOpenCustomHashSet.class.getDeclaredField("n");
        field.setAccessible(true);
        return field.getInt(map);
    }

    @Test
    void cleanupRemovesClearedKeysEvenBeforeQueueNotificationAndShrinksPeakStorage() throws Exception {
        final WeakObject2IntCache<Value> cache = new WeakObject2IntCache<>();
        final List<Value> live = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            final Value key = new Value(i);
            if (i < 50) live.add(key);
            cache.put(key, i - 25);
        }
        final ObjectOpenCustomHashSet<Object> map = map(cache);
        final int before = capacity(map);
        for (final Object key : map) {
            final Reference<?> reference = (Reference<?>) key;
            if (((Value) reference.get()).id >= 50) reference.clear();
        }
        cache.cleanUp();
        assertEquals(50, map.size());
        assertTrue(capacity(map) < before / 8);
        for (final Value key : live) assertEquals(key.id - 25, cache.getCached(new Value(key.id)));
        assertEquals(0, cache.getCached(new Value(25)));
        assertEquals(WeakObject2IntCache.ABSENT, cache.getCached(new Value(9999)));
    }

    @Test
    void boundedMaintenanceServicesIdleQueueWithoutRemovingEqualLiveReplacement() throws Exception {
        final WeakObject2IntCache<Value> cache = new WeakObject2IntCache<>();
        final Value live = new Value(1);
        cache.put(live, 12);
        final Reference<?> stale = (Reference<?>) map(cache).iterator().next();
        stale.clear();
        assertTrue(stale.enqueue());
        cache.put(new Value(1), 34);
        assertEquals(1, cache.maintain(1));
        assertEquals(34, cache.getCached(new Value(1)));
        assertEquals(1, map(cache).size());
    }

    @Test
    void maintenanceIsBoundedAndDoesNotRequireAKeyLookup() throws Exception {
        final WeakObject2IntCache<Value> cache = new WeakObject2IntCache<>();
        for (int i = 0; i < 300; i++) cache.put(new Value(i), i);
        for (final Object key : map(cache)) {
            final Reference<?> reference = (Reference<?>) key;
            reference.clear();
            reference.enqueue();
        }
        assertEquals(10, cache.maintain(10));
        assertEquals(290, map(cache).size());
        assertEquals(0, cache.maintain(0));
        assertEquals(290, cache.maintain(1000));
        assertTrue(map(cache).isEmpty());
    }

    @Test
    void maintenanceStillShrinksAfterLookupsAlreadyDrainedTheQueue() throws Exception {
        final WeakObject2IntCache<Value> cache = new WeakObject2IntCache<>();
        final List<Value> live = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            final Value key = new Value(i);
            live.add(key);
            cache.put(key, i);
        }
        final ObjectOpenCustomHashSet<Object> map = map(cache);
        final int before = capacity(map);
        for (final Object key : map) {
            final Reference<?> reference = (Reference<?>) key;
            if (((Value) reference.get()).id < 600) {
                reference.clear();
                reference.enqueue();
            }
        }
        for (int i = 0; i < 10; i++) cache.getCached(live.get(999));
        assertEquals(400, map.size());
        assertEquals(before, capacity(map), "fixture remains above fastutil's automatic shrink threshold");
        assertEquals(0, cache.maintain(2048), "the queue has already been drained by lookups");
        assertTrue(capacity(map) < before);
        final int after = capacity(map);
        for (int i = 0; i < 10; i++) cache.maintain(2048);
        assertEquals(after, capacity(map));
    }

    private record Value(int id) {
    }

    @Test
    void updatesEqualKeyInPlaceWithoutReplacingItsWeakReference() throws Exception {
        final WeakObject2IntCache<Value> cache = new WeakObject2IntCache<>();
        final Value key = new Value(17);
        cache.put(key, 0);
        final Object reference = map(cache).iterator().next();
        for (final int value : new int[]{Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE}) {
            cache.put(new Value(17), value);
            assertEquals(value, cache.getCached(key));
            assertEquals(1, map(cache).size());
            assertTrue(reference == map(cache).iterator().next());
        }
    }
}
