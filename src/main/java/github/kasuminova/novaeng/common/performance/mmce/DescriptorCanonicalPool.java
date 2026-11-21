package github.kasuminova.novaeng.common.performance.mmce;

import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

/**
 * MMCE descriptor interning, with one lock per hash stripe and an atomic pool reset.
 */
public final class DescriptorCanonicalPool<T> {
    private static final int STRIPES = 16;
    private final ObjectOpenHashSet<T>[] pools;

    @SuppressWarnings("unchecked")
    public DescriptorCanonicalPool() {
        pools = (ObjectOpenHashSet<T>[]) new ObjectOpenHashSet<?>[STRIPES];
        for (int i = 0; i < STRIPES; i++) {
            pools[i] = new ObjectOpenHashSet<>();
        }
    }

    public T canonicalize(final T value) {
        // Equal descriptors select the same stripe. Do not cache hashes: callers can
        // still mutate descriptor lists, just as with MMCE's original pool.
        final ObjectOpenHashSet<T> pool = pools[HashCommon.mix(value.hashCode()) & (STRIPES - 1)];
        synchronized (pool) {
            return pool.addOrGet(value);
        }
    }

    public void clear() {
        clearWithLocks(0);
    }

    private void clearWithLocks(final int index) {
        if (index == STRIPES) {
            for (final ObjectOpenHashSet<T> pool : pools) {
                pool.clear();
            }
            return;
        }
        // Fixed lock order makes concurrent clears safe. Hold every stripe until
        // all are cleared, so no insertion can observe a partially reset pool.
        synchronized (pools[index]) {
            clearWithLocks(index + 1);
        }
    }
}
