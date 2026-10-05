package github.kasuminova.novaeng.common.network.bandwidth;

import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.function.LongConsumer;

/** Server-thread policy for one player's retained chunk watches. */
public final class DelayedChunkCache {
    private final Long2LongLinkedOpenHashMap retained = new Long2LongLinkedOpenHashMap();

    public static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    public static int x(long key) {
        return (int) (key >> 32);
    }

    public static int z(long key) {
        return (int) key;
    }

    public boolean retain(int x, int z, int centerX, int centerZ, int viewRadius, int extraDistance,
                          int limit, long now, long timeoutNanos, LongConsumer evict) {
        if (limit <= 0 || extraDistance <= 0 || outside(x, z, centerX, centerZ, viewRadius + extraDistance)) {
            return false;
        }
        long key = key(x, z);
        retained.remove(key);
        retained.put(key, now);
        expire(centerX, centerZ, viewRadius, extraDistance, limit, now, timeoutNanos, evict);
        return retained.containsKey(key);
    }

    public boolean restore(int x, int z) {
        long key = key(x, z);
        if (!retained.containsKey(key)) {
            return false;
        }
        retained.remove(key);
        return true;
    }

    public boolean contains(int x, int z) {
        return retained.containsKey(key(x, z));
    }

    public void expire(int centerX, int centerZ, int viewRadius, int extraDistance, int limit,
                       long now, long timeoutNanos, LongConsumer evict) {
        var iterator = Long2LongMaps.fastIterator(retained);
        while (iterator.hasNext()) {
            Long2LongMap.Entry entry = iterator.next();
            long key = entry.getLongKey();
            if (now - entry.getLongValue() >= timeoutNanos || outside(x(key), z(key), centerX, centerZ, viewRadius + extraDistance)) {
                iterator.remove();
                evict.accept(key);
            }
        }
        while (retained.size() > limit) {
            LongIterator oldest = retained.keySet().iterator();
            long key = oldest.nextLong();
            oldest.remove();
            evict.accept(key);
        }
    }

    public void clear(LongConsumer evict) {
        LongIterator iterator = retained.keySet().iterator();
        while (iterator.hasNext()) {
            long key = iterator.nextLong();
            iterator.remove();
            evict.accept(key);
        }
    }

    public boolean isEmpty() {
        return retained.isEmpty();
    }

    private static boolean outside(int x, int z, int centerX, int centerZ, int radius) {
        return Math.max(Math.abs((long) x - centerX), Math.abs((long) z - centerZ)) > radius;
    }
}
