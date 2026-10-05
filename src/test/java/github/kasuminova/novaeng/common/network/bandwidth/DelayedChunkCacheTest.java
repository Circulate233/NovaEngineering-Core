package github.kasuminova.novaeng.common.network.bandwidth;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelayedChunkCacheTest {
    @Test
    void restoresARecentlyLeftChunkBeforeTimeout() {
        DelayedChunkCache cache = new DelayedChunkCache();
        LongList evicted = new LongArrayList();
        assertTrue(cache.retain(6, 0, 0, 0, 5, 2, 4, 100, 1_000, evicted::add));
        assertTrue(cache.contains(6, 0));
        assertTrue(cache.restore(6, 0));
        assertFalse(cache.contains(6, 0));
        assertTrue(evicted.isEmpty());
    }

    @Test
    void expiresByTimeoutDistanceAndSize() {
        DelayedChunkCache cache = new DelayedChunkCache();
        LongList evicted = new LongArrayList();
        assertTrue(cache.retain(1, 0, 0, 0, 2, 2, 1, 0, 100, evicted::add));
        assertTrue(cache.retain(2, 0, 0, 0, 2, 2, 1, 1, 100, evicted::add));
        assertFalse(cache.contains(1, 0));
        cache.expire(20, 0, 2, 2, 1, 200, 100, evicted::add);
        assertTrue(cache.isEmpty());
        assertTrue(evicted.size() >= 2);
    }
}
