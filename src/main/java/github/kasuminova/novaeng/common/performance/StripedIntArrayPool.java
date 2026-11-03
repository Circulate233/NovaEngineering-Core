package github.kasuminova.novaeng.common.performance;

import java.util.Arrays;

/**
 * Content interning with one hash computation and independently locked shards. Stored arrays must not be mutated.
 */
public final class StripedIntArrayPool {
    private final Shard[] shards;
    private volatile boolean closed;
    private volatile Statistics finalStatistics;

    public StripedIntArrayPool(final int shardCount, final int expectedSize) {
        if (shardCount < 1 || Integer.bitCount(shardCount) != 1 || expectedSize < 0) {
            throw new IllegalArgumentException("Invalid pool capacity");
        }
        shards = new Shard[shardCount];
        final long perShard = ((long) expectedSize + shardCount - 1) / shardCount;
        int capacity = 2;
        while (capacity * 3L / 4 < perShard) {
            if (capacity == 1 << 30) {
                throw new IllegalArgumentException("Pool capacity too large");
            }
            capacity <<= 1;
        }
        for (int i = 0; i < shardCount; i++) {
            shards[i] = new Shard(capacity);
        }
    }

    public int[] canonicalize(final int[] value) {
        if (closed) {
            return value;
        }
        final int hash = Arrays.hashCode(value);
        final int product = hash * 0x9E3779B9;
        final int mixed = product ^ product >>> 16;
        final Shard shard = shards[(mixed >>> 16) & (shards.length - 1)];
        synchronized (shard) {
            if (closed) {
                return value;
            }
            shard.processed++;
            if (value == null) {
                if (!shard.hasNull) {
                    shard.hasNull = true;
                    shard.size++;
                }
                return null;
            }
            int slot = mixed & (shard.values.length - 1);
            int[] existing;
            while ((existing = shard.values[slot]) != null) {
                if (existing == value || shard.hashes[slot] == mixed && Arrays.equals(existing, value)) {
                    return existing;
                }
                slot = slot + 1 & (shard.values.length - 1);
            }
            shard.values[slot] = value;
            shard.hashes[slot] = mixed;
            shard.size++;
            if (shard.size >= shard.values.length * 3L / 4) {
                shard.grow();
            }
            return value;
        }
    }

    public long processedCount() {
        final Statistics finished = finalStatistics;
        if (finished != null) {
            return finished.processed;
        }
        long total = 0;
        for (final Shard shard : shards) {
            synchronized (shard) {
                total += shard.processed;
            }
        }
        return total;
    }

    /**
     * Waits for accepted insertions, releases array references, and prevents subsequent insertion.
     */
    public synchronized Statistics close() {
        if (finalStatistics != null) {
            return finalStatistics;
        }
        closed = true;
        long processed = 0;
        int unique = 0;
        for (final Shard shard : shards) {
            synchronized (shard) {
                processed += shard.processed;
                unique += shard.size;
                shard.values = null;
                shard.hashes = null;
            }
        }
        finalStatistics = new Statistics(processed, unique);
        return finalStatistics;
    }

    public record Statistics(long processed, int unique) {
    }

    private static final class Shard {
        private int[][] values;
        private int[] hashes;
        private int size;
        private long processed;
        private boolean hasNull;

        private Shard(final int capacity) {
            values = new int[capacity][];
            hashes = new int[capacity];
        }

        private void grow() {
            final int oldCapacity = values.length;
            if (oldCapacity >= 1 << 30) {
                throw new IllegalStateException("Vertex data pool is full");
            }
            final int[][] nextValues = new int[oldCapacity << 1][];
            final int[] nextHashes = new int[nextValues.length];
            final int mask = nextValues.length - 1;
            for (int i = 0; i < oldCapacity; i++) {
                final int[] value = values[i];
                if (value != null) {
                    final int hash = hashes[i];
                    int slot = hash & mask;
                    while (nextValues[slot] != null) {
                        slot = slot + 1 & mask;
                    }
                    nextValues[slot] = value;
                    nextHashes[slot] = hash;
                }
            }
            values = nextValues;
            hashes = nextHashes;
        }
    }
}
