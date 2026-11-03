package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/**
 * Coalesces writes within an AE2 queue drain and always flushes from the caller's finally block.
 */
public final class MeteorWriteBatch implements AutoCloseable {
    private static final ThreadLocal<MeteorWriteBatch> LOCAL = ThreadLocal.withInitial(MeteorWriteBatch::new);
    private static final ReferenceOpenHashSet<MeteorRegionCache> DIRTY = new ReferenceOpenHashSet<>();
    private int depth;

    private MeteorWriteBatch() {
    }

    public static MeteorWriteBatch open() {
        if (!NovaEngCoreConfig.SERVER.batchMeteorSpawnWrites) return null;
        final MeteorWriteBatch batch = LOCAL.get();
        batch.depth++;
        return batch;
    }

    public static boolean active() {
        return NovaEngCoreConfig.SERVER.batchMeteorSpawnWrites && LOCAL.get().depth != 0;
    }

    static void dirty(final MeteorRegionCache cache) {
        synchronized (DIRTY) {
            DIRTY.add(cache);
        }
    }

    static void clean(final MeteorRegionCache cache) {
        synchronized (DIRTY) {
            DIRTY.remove(cache);
        }
    }

    public static void flushAll() {
        flush(true);
    }

    public static void flushReady() {
        flush(false);
    }

    private static void flush(final boolean force) {
        final MeteorRegionCache[] caches;
        synchronized (DIRTY) {
            if (DIRTY.isEmpty()) return;
            caches = DIRTY.toArray(new MeteorRegionCache[0]);
        }
        // Never hold DIRTY while acquiring SpawnData's class monitor.
        for (final MeteorRegionCache cache : caches) cache.flushPending(force);
    }

    @Override
    public void close() {
        if (--depth == 0) flushReady();
    }
}
