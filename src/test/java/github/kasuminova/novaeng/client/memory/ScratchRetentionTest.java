package github.kasuminova.novaeng.client.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScratchRetentionTest {
    @Test
    void requiresTwoCompleteLowUseWindows() {
        final ScratchRetention policy = new ScratchRetention();
        for (int i = 0; i < 127; i++) {
            assertFalse(policy.endTask(100, 1 << 20, 64 << 10));
        }
        assertTrue(policy.endTask(100, 1 << 20, 64 << 10));
        assertFalse(policy.endTask(100, 0, 64 << 10));
    }

    @Test
    void anActualLargeUseRestartsTheHysteresis() {
        final ScratchRetention policy = new ScratchRetention();
        for (int i = 0; i < 64; i++) { assertFalse(policy.endTask(0, 1 << 20, 64 << 10)); }
        assertFalse(policy.endTask(1 << 20, 1 << 20, 64 << 10));
        for (int i = 0; i < 127; i++) { assertFalse(policy.endTask(0, 1 << 20, 64 << 10)); }
        for (int i = 0; i < 63; i++) { assertFalse(policy.endTask(0, 1 << 20, 64 << 10)); }
        assertTrue(policy.endTask(0, 1 << 20, 64 << 10));
    }

    @Test
    void neverReclaimsNormalInitialCapacity() {
        final ScratchRetention policy = new ScratchRetention();
        for (int i = 0; i < 1000; i++) { assertFalse(policy.endTask(0, 64 << 10, 64 << 10)); }
    }
}
