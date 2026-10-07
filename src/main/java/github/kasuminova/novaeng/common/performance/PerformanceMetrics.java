package github.kasuminova.novaeng.common.performance;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Optional counters; disabled hot paths perform only one volatile read. No worlds or tiles are retained.
 */
public final class PerformanceMetrics {
    private static final AtomicLongArray VALUES = new AtomicLongArray(Counter.values().length);
    public static volatile boolean enabled;
    private PerformanceMetrics() {
    }

    public static void add(final Counter counter, final long amount) {
        if (enabled) {
            VALUES.addAndGet(counter.ordinal(), amount);
        }
    }

    public static long get(final Counter counter) {
        return VALUES.get(counter.ordinal());
    }

    public static void reset() {
        for (int i = 0; i < VALUES.length(); i++) {
            VALUES.set(i, 0);
        }
    }

    public enum Counter {
        INVENTORY_SLOTS, INVENTORY_CRITERIA, INVENTORY_ITEM_MISSES,
        RECIPE_POOL_BORROWS, RECIPE_POOL_HITS, RECIPE_POOL_DISCARDS,
        SECTION_DECODED, SECTION_ENCODED, SECTION_STATES,
        MMCE_NOTIFICATIONS, MMCE_NOTIFICATIONS_SKIPPED, AE_OUTPUT_INSERTS,
        BUILD_DEFERRED, PENDING_TE_LOOKUPS, PENDING_TE_SCANNED,
        SAVE_COUNT, SAVE_NANOS
    }
}
