package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.border.WorldBorder;

import java.util.Random;

public final class SpawnChunkCandidates {

    private final LongArrayList positions = new LongArrayList(225);
    private LongOpenHashSet eligible;
    private boolean indexed;
    private int capChunkCount;

    public void clear() {
        this.clear(false);
    }

    public void clear(final boolean multiplePlayers) {
        this.positions.clear();
        this.indexed = false;
        this.capChunkCount = 0;
        if (multiplePlayers) {
            this.indexPositions();
        }
    }

    public void addPlayer(final int centerX, final int centerZ, final WorldBorder border,
                          final PlayerChunkMap chunkMap, final boolean defaultBorder) {
        this.addPlayer(centerX, centerZ, border, chunkMap, defaultBorder, null);
    }

    void addPlayer(final int centerX, final int centerZ, final ChunkFilter filter) {
        this.addPlayer(centerX, centerZ, null, null, false, filter);
    }

    private void addPlayer(final int centerX, final int centerZ, final WorldBorder border,
                           final PlayerChunkMap chunkMap, final boolean defaultBorder, final ChunkFilter filter) {
        if (this.positions.isEmpty()) {
            this.capChunkCount += 289;
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    final int x = centerX + dx;
                    final int z = centerZ + dz;
                    if (this.isEligible(x, z, border, chunkMap, defaultBorder, filter)) {
                        final long key = (x & 0xFFFFFFFFL) | ((long) z << 32);
                        this.positions.add(key);
                        if (this.indexed) {
                            this.eligible.add(key);
                        }
                    }
                }
            }
            return;
        }

        if (!this.indexed) {
            this.indexPositions();
        }

        for (int dx = -8; dx <= 8; dx++) {
            final int x = centerX + dx;
            for (int dz = -8; dz <= 8; dz++) {
                final int z = centerZ + dz;
                final long key = (x & 0xFFFFFFFFL) | ((long) z << 32);
                if (this.eligible.contains(key)) {
                    continue;
                }

                // Vanilla only deduplicates eligible chunks, not the outer ring or rejected chunks.
                this.capChunkCount++;
                if (dx != -8 && dx != 8 && dz != -8 && dz != 8
                    && this.isEligible(x, z, border, chunkMap, defaultBorder, filter)) {
                    this.eligible.add(key);
                    this.positions.add(key);
                }
            }
        }
    }

    private void indexPositions() {
        if (this.eligible == null) {
            this.eligible = new LongOpenHashSet(Math.max(225, this.positions.size()));
        } else {
            this.eligible.clear();
        }
        final long[] keys = this.positions.elements();
        for (int i = 0; i < this.positions.size(); i++) {
            this.eligible.add(keys[i]);
        }
        this.indexed = true;
    }

    private boolean isEligible(final int x, final int z, final WorldBorder border,
                               final PlayerChunkMap chunkMap, final boolean defaultBorder, final ChunkFilter filter) {
        if (filter != null) {
            return filter.test(x, z);
        }
        if (defaultBorder) {
            if (!((x << 4) + 15 > border.minX() && (x << 4) < border.maxX()
                && (z << 4) + 15 > border.minZ() && (z << 4) < border.maxZ())) {
                return false;
            }
        } else if (!border.contains(new ChunkPos(x, z))) {
            return false;
        }
        final PlayerChunkMapEntry entry = chunkMap.getEntry(x, z);
        return entry != null && entry.isSentToPlayers();
    }

    @FunctionalInterface
    interface ChunkFilter {
        boolean test(int x, int z);
    }

    public int capChunkCount() {
        return this.capChunkCount;
    }

    public int size() {
        return this.positions.size();
    }

    /**
     * The returned backing array is valid only up to {@link #size()} and is reused by later calls.
     */
    public long[] shuffle(final Random random) {
        final long[] keys = this.positions.elements();
        final int size = this.positions.size();
        for (int i = size; i > 1; i--) {
            final int other = random.nextInt(i);
            final long key = keys[i - 1];
            keys[i - 1] = keys[other];
            keys[other] = key;
        }
        return keys;
    }
}
