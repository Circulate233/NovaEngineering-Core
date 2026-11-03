package github.kasuminova.novaeng.common.util.collection;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * A thread-confined memo cache with weak keys, primitive values and allocation-free lookups.
 */
public final class WeakObject2IntCache<K> {
    public static final long ABSENT = Long.MIN_VALUE;
    private final ReferenceQueue<K> collected = new ReferenceQueue<>();
    private final Object2IntOpenCustomHashMap<Object> values = new Object2IntOpenCustomHashMap<>(128, new KeyStrategy());

    public long getCached(final K key) {
        // Bound work after a large collection; later lookups continue draining the queue.
        for (int remaining = 64; remaining > 0; remaining--) {
            final var stale = collected.poll();
            if (stale == null) break;
            values.removeInt(stale);
        }
        final int value = values.getInt(key);
        return value != 0 || values.containsKey(key) ? value : ABSENT;
    }

    public void put(final K key, final int value) {
        values.put(new Key<>(key, collected), value);
    }

    private static final class Key<K> extends WeakReference<K> {
        private final int hash;

        private Key(final K value, final ReferenceQueue<K> queue) {
            super(value, queue);
            hash = value.hashCode();
        }
    }

    private static final class KeyStrategy implements Hash.Strategy<Object> {
        @Override
        public int hashCode(final Object key) {
            return key instanceof Key<?> weak ? weak.hash : key == null ? 0 : key.hashCode();
        }

        @Override
        public boolean equals(final Object left, final Object right) {
            if (left == right) return true;
            final Object a = left instanceof Key<?> weak ? weak.get() : left;
            final Object b = right instanceof Key<?> weak ? weak.get() : right;
            return a != null && a.equals(b);
        }
    }
}
