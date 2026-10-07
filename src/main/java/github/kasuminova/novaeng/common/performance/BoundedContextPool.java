package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Bounded idle storage. Borrowers own reset, construction and destruction outside its monitor.
 */
public final class BoundedContextPool<K, V> {
    private final Object2ObjectOpenHashMap<K, ObjectArrayList<V>> idle = new Object2ObjectOpenHashMap<>();
    private int generation;
    private int size;

    public synchronized int generation() {
        return generation;
    }

    public synchronized V take(final K key, final int expectedGeneration) {
        if (generation != expectedGeneration) {
            return null;
        }
        final ObjectArrayList<V> values = idle.get(key);
        if (values == null) {
            return null;
        }
        final V value = values.removeLast();
        size--;
        if (values.isEmpty()) {
            idle.remove(key);
        }
        return value;
    }

    /**
     * A generation captured before resetting a returned value rejects late returns after reload.
     */
    public synchronized boolean offer(final K key, final V value, final int expectedGeneration,
                                      final int perKeyLimit, final int totalLimit) {
        if (generation != expectedGeneration || perKeyLimit <= 0 || size >= totalLimit) {
            return false;
        }
        ObjectArrayList<V> values = idle.get(key);
        if (values != null && values.size() >= perKeyLimit) {
            return false;
        }
        if (values == null) {
            values = new ObjectArrayList<>(Math.min(perKeyLimit, 8));
            idle.put(key, values);
        }
        values.add(value);
        size++;
        return true;
    }

    /**
     * Detaches all idle values atomically; their disposal must happen outside this lock.
     */
    public synchronized ObjectArrayList<V> reset() {
        generation++;
        final ObjectArrayList<V> detached = new ObjectArrayList<>(size);
        for (final ObjectArrayList<V> values : idle.values()) {
            detached.addAll(values);
        }
        idle.clear();
        size = 0;
        return detached;
    }

    public synchronized int size() {
        return size;
    }
}
