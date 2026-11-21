package github.kasuminova.novaeng.common.util.collection;

import java.lang.ref.WeakReference;
import java.util.List;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Services ingredient memo caches on their owning client thread, without retaining them.
 */
public final class WeakCacheMaintenance {
    private static final List<Registration> CACHES = new ObjectArrayList<>();

    private WeakCacheMaintenance() {
    }

    public static <K> WeakObject2IntCache<K> register(final WeakObject2IntCache<K> cache) {
        synchronized (CACHES) {
            CACHES.add(new Registration(new WeakReference<>(cache), Thread.currentThread()));
        }
        return cache;
    }

    public static void tick() {
        maintain(false);
    }

    public static void loadingComplete() {
        maintain(true);
    }

    private static void maintain(final boolean full) {
        synchronized (CACHES) {
            for (int i = CACHES.size() - 1; i >= 0; i--) {
                final Registration registration = CACHES.get(i);
                final WeakObject2IntCache<?> cache = registration.cache.get();
                if (cache == null) {
                    CACHES.remove(i);
                } else if (registration.owner == Thread.currentThread()) {
                    if (full) cache.cleanUp();
                    else cache.maintain(2048);
                }
            }
        }
    }

    private record Registration(WeakReference<WeakObject2IntCache<?>> cache, Thread owner) {
    }
}
