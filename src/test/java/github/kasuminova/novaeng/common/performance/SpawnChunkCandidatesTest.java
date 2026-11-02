package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnChunkCandidatesTest {

    private static Set<Long> keys(final SpawnChunkCandidates candidates) {
        final Set<Long> keys = new HashSet<>();
        final long[] values = candidates.shuffle(new Random(0));
        for (int i = 0; i < candidates.size(); i++) {
            assertTrue(keys.add(values[i]), "duplicate candidate");
        }
        return keys;
    }

    private static long pack(final int x, final int z) {
        return (x & 0xFFFFFFFFL) | ((long) z << 32);
    }

    @Test
    void singlePlayerKeepsOuterRingOnlyInCapCount() {
        final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
        candidates.addPlayer(-3, 8, (x, z) -> true);
        assertEquals(289, candidates.capChunkCount());
        assertEquals(225, candidates.size());
        assertTrue(keys(candidates).contains(pack(-10, 1)));
    }

    @Test
    void overlappingPlayersKeepVanillasRepeatedOuterRingCount() {
        final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
        candidates.addPlayer(0, 0, (x, z) -> true);
        candidates.addPlayer(0, 0, (x, z) -> true);
        assertEquals(353, candidates.capChunkCount());
        assertEquals(225, candidates.size());
    }

    @Test
    void rejectedChunksAreReconsideredForLaterPlayers() {
        final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
        candidates.addPlayer(0, 0, (x, z) -> false);
        candidates.addPlayer(0, 0, (x, z) -> true);
        assertEquals(578, candidates.capChunkCount());
        assertEquals(225, candidates.size());
    }

    @Test
    void matchesVanillaAcrossOverlapsRejectionsAndSignedCoordinates() {
        final Random random = new Random(0xC5F71L);
        for (int trial = 0; trial < 600; trial++) {
            final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
            final Reference reference = new Reference();
            final int salt = random.nextInt();
            final List<Long> expectedQueries = new ArrayList<>();
            final List<Long> actualQueries = new ArrayList<>();
            final int centerX = random.nextInt();
            final int centerZ = random.nextInt();
            for (int player = 0, count = 1 + random.nextInt(8); player < count; player++) {
                final int x = centerX + random.nextInt(41) - 20;
                final int z = centerZ + random.nextInt(41) - 20;
                final int phase = player;
                reference.addPlayer(x, z, (cx, cz) -> {
                    expectedQueries.add(pack(cx, cz));
                    return ((cx * 31 + cz * 17 + salt + phase) & 7) < 5;
                });
                candidates.addPlayer(x, z, (cx, cz) -> {
                    actualQueries.add(pack(cx, cz));
                    return ((cx * 31 + cz * 17 + salt + phase) & 7) < 5;
                });
                assertEquals(reference.capCount, candidates.capChunkCount(), "trial " + trial);
                assertEquals(reference.keys(), keys(candidates), "trial " + trial);
                assertEquals(expectedQueries, actualQueries, "filter query order, trial " + trial);
            }
        }
    }

    @Test
    void packedCoordinatesKeepBothSignedComponentsAtIntegerEdges() {
        for (final int center : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0, 1}) {
            final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
            final Reference reference = new Reference();
            reference.addPlayer(center, -center, (x, z) -> true);
            candidates.addPlayer(center, -center, (x, z) -> true);
            assertEquals(reference.keys(), keys(candidates));
        }
    }

    @Test
    void shuffleMutatesAndReusesTheCurrentPermutation() {
        final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
        candidates.addPlayer(0, 0, (x, z) -> true);
        final List<Long> expected = new ArrayList<>();
        for (int x = -7; x <= 7; x++) {
            for (int z = -7; z <= 7; z++) {
                expected.add(pack(x, z));
            }
        }
        Collections.shuffle(expected, new Random(42));
        final long[] buffer = candidates.shuffle(new Random(42));
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).longValue(), buffer[i]);
        }
        Collections.shuffle(expected, new Random(7));
        assertSame(buffer, candidates.shuffle(new Random(7)));
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).longValue(), buffer[i]);
        }
        assertEquals(225, keys(candidates).size());
        candidates.clear();
        assertEquals(0, candidates.capChunkCount());
        assertEquals(0, candidates.size());
        candidates.addPlayer(20, -20, (x, z) -> (x & 1) == 0);
        assertEquals(105, candidates.size());
        assertEquals(105, keys(candidates).size());
    }

    @Test
    void lookupIsRebuiltAfterShufflingAndAcrossPlayerCountChanges() {
        final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
        for (int round = 0; round < 80; round++) {
            candidates.clear();
            final Reference reference = new Reference();
            final int offset = round * 40;
            final SpawnChunkCandidates.ChunkFilter filter = (x, z) -> ((x + z) & 3) != 0;
            for (int player = 0, count = 1 + round % 4; player < count; player++) {
                reference.addPlayer(offset + player, -offset, filter);
                candidates.addPlayer(offset + player, -offset, filter);
                assertEquals(reference.capCount, candidates.capChunkCount());
                assertEquals(reference.keys(), keys(candidates));
            }
        }
    }

    @Test
    void knownPlayerCountAndLazyLookupProduceTheSameCandidates() {
        final SpawnChunkCandidates eager = new SpawnChunkCandidates();
        final SpawnChunkCandidates lazy = new SpawnChunkCandidates();
        for (int round = 0; round < 60; round++) {
            eager.clear(round % 2 == 0);
            lazy.clear();
            final Reference reference = new Reference();
            final int offset = round * 40;
            final SpawnChunkCandidates.ChunkFilter filter = (x, z) -> ((x + z) & 3) != 0;
            for (int player = 0, count = 1 + round % 4; player < count; player++) {
                reference.addPlayer(offset + player, -offset, filter);
                eager.addPlayer(offset + player, -offset, filter);
                lazy.addPlayer(offset + player, -offset, filter);
                assertEquals(reference.capCount, eager.capChunkCount());
                assertEquals(reference.capCount, lazy.capChunkCount());
                assertEquals(reference.keys(), keys(eager));
                assertEquals(reference.keys(), keys(lazy));
            }
        }
    }

    @Test
    void emptyAndSingleElementShuffleDoNotConsumeRandomValues() {
        final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
        final Random random = new Random(42);
        final Random expected = new Random(42);
        candidates.shuffle(random);
        assertEquals(expected.nextLong(), random.nextLong());
        candidates.addPlayer(0, 0, (x, z) -> x == 0 && z == 0);
        final long[] buffer = candidates.shuffle(random);
        assertEquals(1, candidates.size());
        assertEquals(pack(0, 0), buffer[0]);
        assertEquals(expected.nextLong(), random.nextLong());
    }

    @Test
    void grownStorageIsReusedAndOldTailIsExcludedAfterClear() {
        final SpawnChunkCandidates candidates = new SpawnChunkCandidates();
        candidates.addPlayer(0, 0, (x, z) -> true);
        candidates.addPlayer(40, 40, (x, z) -> true);
        final long[] grown = candidates.shuffle(new Random(42));
        assertEquals(450, candidates.size());
        assertEquals(450, keys(candidates).size());
        candidates.clear();
        candidates.addPlayer(0, 0, (x, z) -> x == 0 && z == 0);
        assertSame(grown, candidates.shuffle(new Random(7)));
        assertEquals(1, candidates.size());
        assertEquals(Set.of(pack(0, 0)), keys(candidates));
        candidates.addPlayer(40, 40, (x, z) -> x == 40 && z == 40);
        assertEquals(Set.of(pack(0, 0), pack(40, 40)), keys(candidates));
    }

    private record Position(int x, int z) {
    }

    private static final class Reference {
        private final Set<Position> eligible = new HashSet<>();
        private int capCount;

        private void addPlayer(final int centerX, final int centerZ, final SpawnChunkCandidates.ChunkFilter filter) {
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    final Position position = new Position(centerX + dx, centerZ + dz);
                    if (!this.eligible.contains(position)) {
                        this.capCount++;
                        if (dx != -8 && dx != 8 && dz != -8 && dz != 8 && filter.test(position.x(), position.z())) {
                            this.eligible.add(position);
                        }
                    }
                }
            }
        }

        private Set<Long> keys() {
            final Set<Long> keys = new HashSet<>();
            for (final Position position : this.eligible) {
                keys.add(pack(position.x(), position.z()));
            }
            return keys;
        }
    }
}
