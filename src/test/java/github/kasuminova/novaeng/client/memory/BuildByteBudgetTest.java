package github.kasuminova.novaeng.client.memory;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildByteBudgetTest {
    @Test
    void reservationStaysChargedUntilCompletionThenOutputDisposal() {
        final BuildByteBudget budget = new BuildByteBudget();
        final long limit = 8L << 20;
        assertTrue(budget.canSchedule(limit));
        final long reservation = budget.reserve();
        assertFalse(budget.canSchedule(limit));
        budget.complete(reservation, 16L << 20);
        assertFalse(budget.canSchedule(limit));
        assertEquals(0, budget.snapshot().reservedBytes());
        assertEquals(16L << 20, budget.snapshot().completedBytes());
        budget.releaseOutput(16L << 20);
        assertTrue(budget.canSchedule(limit));
        assertTrue(budget.reserve() > limit);
    }

    @Test
    void canceledOrFailedJobsReleaseTheirReservationWithoutProducingOutput() {
        final BuildByteBudget budget = new BuildByteBudget();
        final long reservation = budget.reserve();
        budget.complete(reservation, 0);
        assertEquals(0, budget.snapshot().inFlight());
        assertTrue(budget.canSchedule(1));
        assertThrows(IllegalStateException.class, () -> budget.complete(reservation, 0));
    }

    @Test
    void concurrentCompletionsAndDisposalsBalance() throws Exception {
        final BuildByteBudget budget = new BuildByteBudget();
        try (var workers = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 100; i++) {
                final long reserved = budget.reserve();
                workers.submit(() -> {
                    budget.complete(reserved, 1000);
                    budget.releaseOutput(1000);
                });
            }
            workers.shutdown();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
        assertEquals(0, budget.snapshot().reservedBytes());
        assertEquals(0, budget.snapshot().completedBytes());
        assertEquals(0, budget.snapshot().inFlight());
    }
}
