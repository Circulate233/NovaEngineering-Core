package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnCountLimiterTest {

    @Test
    void retainsInclusiveCapAndRejectsNegativeCaps() {
        assertTrue(SpawnCountLimiter.isWithinLimit(List.of(true, false, true), value -> value, 2));
        assertFalse(SpawnCountLimiter.isWithinLimit(List.of(true, false, true), value -> value, 1));
        assertTrue(SpawnCountLimiter.isWithinLimit(List.<Boolean>of(), value -> value, 0));
        assertFalse(SpawnCountLimiter.isWithinLimit(List.<Boolean>of(), value -> value, -1));
    }

    @Test
    void stopsImmediatelyAfterLimitIsExceeded() {
        final AtomicInteger calls = new AtomicInteger();
        assertFalse(SpawnCountLimiter.isWithinLimit(List.of(true, true, true, true, true), value -> {
            calls.incrementAndGet();
            return value;
        }, 2));
        assertEquals(3, calls.get());
    }

    @Test
    void readsCurrentEntitiesAndClassificationEveryTime() {
        final List<boolean[]> entities = new ArrayList<>();
        entities.add(new boolean[]{true, false});
        assertTrue(SpawnCountLimiter.isWithinLimit(entities, entity -> entity[0], 1));
        entities.add(new boolean[]{true, true});
        assertFalse(SpawnCountLimiter.isWithinLimit(entities, entity -> entity[0], 1));
        entities.getFirst()[0] = false;
        assertTrue(SpawnCountLimiter.isWithinLimit(entities, entity -> entity[0], 1));
        assertFalse(SpawnCountLimiter.isWithinLimit(entities, entity -> entity[1], 0));
        entities.removeLast();
        assertTrue(SpawnCountLimiter.isWithinLimit(entities, entity -> entity[1], 0));
    }

    @Test
    void matchesFullCountsForRandomClassificationsAndCaps() {
        final Random random = new Random(0x77192L);
        for (int trial = 0; trial < 2000; trial++) {
            final List<Integer> entities = new ArrayList<>();
            for (int i = 0, count = random.nextInt(500); i < count; i++) {
                entities.add(random.nextInt());
            }
            final int typeMask = 1 << random.nextInt(4);
            final int limit = random.nextInt(300) - 1;
            final long fullCount = entities.stream().filter(entity -> (entity & typeMask) != 0).count();
            assertEquals(fullCount <= limit,
                SpawnCountLimiter.isWithinLimit(entities, entity -> (entity & typeMask) != 0, limit));
        }
    }
}
