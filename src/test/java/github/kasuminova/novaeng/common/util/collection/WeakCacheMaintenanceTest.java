package github.kasuminova.novaeng.common.util.collection;

import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeakCacheMaintenanceTest {
    private static ObjectOpenCustomHashSet<?> map(final WeakObject2IntCache<?> cache) throws Exception {
        final Field field = WeakObject2IntCache.class.getDeclaredField("values");
        field.setAccessible(true);
        return (ObjectOpenCustomHashSet<?>) field.get(cache);
    }

    @Test
    void loadingBoundaryAndIdleTickCleanWithoutFurtherIngredientLookups() throws Exception {
        final WeakObject2IntCache<Object> cache = WeakCacheMaintenance.register(new WeakObject2IntCache<>());
        cache.put(new Object(), 13);
        final ObjectOpenCustomHashSet<?> map = map(cache);
        final Reference<?> stale = (Reference<?>) map.iterator().next();
        stale.clear();
        // The load boundary sweeps references even when the ReferenceHandler has not enqueued them.
        WeakCacheMaintenance.loadingComplete();
        assertTrue(map.isEmpty());

        cache.put(new Object(), 47);
        final Reference<?> queued = (Reference<?>) map.iterator().next();
        queued.clear();
        queued.enqueue();
        WeakCacheMaintenance.tick();
        assertTrue(map.isEmpty());
    }

    @Test
    void maintenanceDoesNotTouchACacheFromAnotherThread() throws Exception {
        final WeakObject2IntCache<Object> cache = WeakCacheMaintenance.register(new WeakObject2IntCache<>());
        cache.put(new Object(), 13);
        final ObjectOpenCustomHashSet<?> map = map(cache);
        final Reference<?> stale = (Reference<?>) map.iterator().next();
        stale.clear();
        stale.enqueue();
        final Thread other = Thread.ofPlatform().start(WeakCacheMaintenance::loadingComplete);
        other.join();
        assertEquals(1, map.size());
        WeakCacheMaintenance.tick();
        assertTrue(map.isEmpty());
    }
}
