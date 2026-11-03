package github.kasuminova.novaeng.client.memory;

/**
 * Accessed while holding MMCE's existing BufferBuilderPool class monitor.
 */
public final class MmceBufferPoolBudget {
    private static final long MAX_IDLE_BYTES = 64L * 1024 * 1024;
    private static long idleBytes;

    private MmceBufferPoolBudget() {
    }

    public static boolean retain(final int capacity) {
        if (capacity > MAX_IDLE_BYTES - idleBytes) return false;
        idleBytes += capacity;
        return true;
    }

    public static void borrowed(final int capacity) {
        idleBytes -= capacity;
    }

    public static void cleared() {
        idleBytes = 0;
    }
}
