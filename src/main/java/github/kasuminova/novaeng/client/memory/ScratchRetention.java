package github.kasuminova.novaeng.client.memory;

/** Owner-thread hysteresis: release only after two full low-utilization task windows. */
public final class ScratchRetention {
    private int samples;
    private long peak;
    private boolean previousLow;

    public boolean endTask(final long usedBytes, final long capacity, final long initialCapacity) {
        peak = Math.max(peak, usedBytes);
        if (++samples < 64) {
            return false;
        }
        final long allowance = Math.max(initialCapacity, peak > Long.MAX_VALUE / 4 ? Long.MAX_VALUE : peak * 4);
        final boolean low = capacity > allowance;
        final boolean release = low && previousLow;
        previousLow = low && !release;
        samples = 0;
        peak = 0;
        return release;
    }
}
