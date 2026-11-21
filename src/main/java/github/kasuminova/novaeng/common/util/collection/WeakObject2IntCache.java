package github.kasuminova.novaeng.common.util.collection;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * A thread-confined memo cache with weak keys, primitive values and allocation-free lookups.
 */
public final class WeakObject2IntCache<K> {
    public static final long ABSENT = Long.MIN_VALUE;
    private static final int INITIAL_SIZE = 128;
    private final ReferenceQueue<K> collected = new ReferenceQueue<>();
    private final ObjectOpenCustomHashSet<Object> values = new ObjectOpenCustomHashSet<>(INITIAL_SIZE, new KeyStrategy());
    private int sizeAtLastTrim = INITIAL_SIZE;

    public long getCached(final K key) {
        // Bound work after a large collection; later lookups continue draining the queue.
        drainCollected(64);
        final Key<?> cached = (Key<?>) values.get(key);
        return cached == null ? ABSENT : cached.value;
    }

    public void put(final K key, final int value) {
        final Key<?> cached = (Key<?>) values.get(key);
        if (cached != null) {
            cached.value = value;
            return;
        }
        values.add(new Key<>(key, collected, value));
        sizeAtLastTrim = Math.max(sizeAtLastTrim, values.size());
    }

    /**
     * A bounded maintenance pass for the same thread that owns this cache. The queue
     * must also be serviced when no ingredient lookups are being performed.
     */
    public int maintain(final int limit) {
        if (limit <= 0) return 0;
        final int removed = drainCollected(limit);
        // Lookups can already have drained the queue since the previous tick. The
        // high-water mark, rather than this pass's removal count, determines shrinkage.
        if (sizeAtLastTrim > INITIAL_SIZE && values.size() <= sizeAtLastTrim / 2) {
            values.trim(Math.max(INITIAL_SIZE, values.size()));
            sizeAtLastTrim = Math.max(INITIAL_SIZE, values.size());
        }
        return removed;
    }

    /**
     * Fully clean at a loading boundary, including cleared references not yet enqueued.
     */
    public void cleanUp() {
        drainCollected(Integer.MAX_VALUE);
        values.removeIf(o -> ((Key<?>) o).get() == null);
        values.trim(Math.max(INITIAL_SIZE, values.size()));
        sizeAtLastTrim = Math.max(INITIAL_SIZE, values.size());
    }

    private int drainCollected(final int limit) {
        int removed = 0;
        for (int remaining = limit; remaining > 0; remaining--) {
            final var stale = collected.poll();
            if (stale == null) break;
            if (values.remove(stale)) removed++;
        }
        return removed;
    }

    private static final class Key<K> extends WeakReference<K> {
        private final int hash;
        // Fits the padding beside hash on the measured 64-bit object layout;
        // avoids a separate int[] indexed in parallel with the weak keys.
        private int value;

        private Key(final K key, final ReferenceQueue<K> queue, final int value) {
            super(key, queue);
            hash = key.hashCode();
            this.value = value;
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
