package github.kasuminova.novaeng.common.performance;

/**
 * Each caller owns its numerical scratch through ThreadLocal; no thread registry or shared lock is needed.
 */
public final class ThreadConfinedTemperatureCache {
    private final ThreadLocal<AtmosphereTemperatureCache> cache =
        ThreadLocal.withInitial(AtmosphereTemperatureCache::new);

    public int get(
        final int temperature, final float size, final int distance, final int pressure) {
        return cache.get().get(temperature, size, distance, pressure);
    }
}
