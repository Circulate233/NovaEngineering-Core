package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedContextPoolTest {
    @Test
    void enforcesBothBoundsAndRemovesEmptyBuckets() {
        final BoundedContextPool<String, Object> pool = new BoundedContextPool<>();
        final Object a = new Object();
        final Object b = new Object();
        assertTrue(pool.offer("a", a, 0, 1, 2));
        assertFalse(pool.offer("a", new Object(), 0, 1, 2));
        assertTrue(pool.offer("b", b, 0, 1, 2));
        assertFalse(pool.offer("c", new Object(), 0, 1, 2));
        assertSame(a, pool.take("a", 0));
        assertNull(pool.take("a", 0));
        assertTrue(pool.offer("c", a, 0, 1, 2));
        assertEquals(2, pool.size());
    }

    @Test
    void rejectsReturnsWhichCrossReload() {
        final BoundedContextPool<String, Object> pool = new BoundedContextPool<>();
        final int oldGeneration = pool.generation();
        final Object value = new Object();
        pool.offer("a", value, oldGeneration, 8, 1024);
        assertSame(value, pool.reset().getFirst());
        assertFalse(pool.offer("a", value, oldGeneration, 8, 1024));
        assertNull(pool.take("a", oldGeneration));
        assertTrue(pool.offer("a", value, pool.generation(), 8, 1024));
    }

    @Test
    void concurrentOffersCannotExceedTheGlobalLimit() throws Exception {
        final BoundedContextPool<Integer, Integer> pool = new BoundedContextPool<>();
        final CountDownLatch ready = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (int worker = 0; worker < 8; worker++) {
                final int key = worker;
                executor.submit(() -> {
                    ready.await();
                    for (int i = 0; i < 1000; i++) {
                        pool.offer(key, i, 0, 32, 100);
                    }
                    return null;
                });
            }
            ready.countDown();
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(100, pool.size());
        assertEquals(100, pool.reset().size());
        assertEquals(0, pool.size());
    }
}
