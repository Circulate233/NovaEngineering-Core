package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AtmosphereTemperatureCacheTest {

    static Method temperatureMethod(final ClassLoader loader) throws ReflectiveOperationException {
        return loader.loadClass("zmaster587.advancedRocketry.util.AstronomicalBodyHelper")
                     .getMethod("getAverageTemperature", StellarBody.class, int.class, int.class);
    }

    @Test
    void actualReplacementMatchesOriginalHelperForLiveInputChangesAndEdgeCases() throws Exception {
        final Method original = temperatureMethod(new WorldgenFixtureLoader(false));
        final Method optimized = temperatureMethod(new WorldgenFixtureLoader(true));
        final StellarBody star = new StellarBody();
        final Random random = new Random(0x5A9B7L);
        final float[] edges = {0.0F, -0.0F, 1.0F, -1.0F, Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.MIN_VALUE, Float.MAX_VALUE};
        for (int i = 0; i < 5000; i++) {
            star.setTemperature(random.nextInt());
            star.setSize(i < edges.length ? edges[i] : Float.intBitsToFloat(random.nextInt()));
            final int distance = i % 7 == 0 ? 0 : random.nextInt();
            final int pressure = i % 11 == 0 ? 0 : random.nextInt();
            final Object expected = original.invoke(null, star, distance, pressure);
            assertEquals(expected, optimized.invoke(null, star, distance, pressure), "input " + i);
            assertEquals(expected, optimized.invoke(null, star, distance, pressure), "cache hit " + i);
        }
    }

    @Test
    void cachedCallsStillReadBothLiveStellarGettersOnceInOrder() throws Exception {
        final Method optimized = temperatureMethod(new WorldgenFixtureLoader(true));
        final StringBuilder calls = new StringBuilder();
        final StellarBody star = new StellarBody() {
            @Override
            public int getTemperature() {
                calls.append('T');
                return 100;
            }

            @Override
            public float getSize() {
                calls.append('S');
                return 1.0F;
            }
        };
        for (int i = 0; i < 3; i++) {
            optimized.invoke(null, star, 100, 100);
        }
        assertEquals("TSTSTS", calls.toString());
    }

    @Test
    void threadLocalCachesStayCorrectUnderConcurrentInputChanges() throws Exception {
        final ThreadLocal<AtmosphereTemperatureCache> caches = ThreadLocal.withInitial(AtmosphereTemperatureCache::new);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread[] workers = new Thread[4];
        for (int worker = 0; worker < workers.length; worker++) {
            final int base = worker;
            workers[worker] = new Thread(() -> {
                try {
                    final AtmosphereTemperatureCache cache = caches.get();
                    for (int i = 0; i < 2000; i++) {
                        final int temperature = base * 100 + i;
                        assertEquals(AtmosphereTemperatureCache.calculate(temperature, 1.0F, 100, i),
                            cache.get(temperature, 1.0F, 100, i));
                    }
                } catch (final Throwable throwable) {
                    failure.compareAndSet(null, throwable);
                }
            });
            workers[worker].start();
        }
        for (final Thread worker : workers) {
            worker.join();
        }
        assertNull(failure.get());
    }

    @Test
    void classificationRedirectsRetainStrictThresholdsAndDoNotExposePrivateArrays() throws Exception {
        for (final String[] target : new String[][]{
            {"AtmosphereTypes", "getAtmosphereTypeFromValue"}, {"Temps", "getTempFromValue"}}) {
            final Class<?> original = new WorldgenFixtureLoader(false)
                .loadClass("zmaster587.advancedRocketry.dimension.DimensionProperties$" + target[0]);
            final Class<?> optimized = new WorldgenFixtureLoader(true).loadClass(original.getName());
            final Method before = original.getMethod(target[1], int.class);
            final Method after = optimized.getMethod(target[1], int.class);
            for (int value = -1000; value <= 2000; value++) {
                assertEquals(((Enum<?>) before.invoke(null, value)).name(), ((Enum<?>) after.invoke(null, value)).name());
            }
            for (final int value : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}) {
                assertEquals(((Enum<?>) before.invoke(null, value)).name(), ((Enum<?>) after.invoke(null, value)).name());
            }
            final Object[] external = (Object[]) optimized.getMethod("values").invoke(null);
            external[0] = null;
            assertNotNull(after.invoke(null, Integer.MAX_VALUE));
        }
    }
}
