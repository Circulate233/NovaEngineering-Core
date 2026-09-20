package github.kasuminova.novaeng.client.texture;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotCapacityFrontierImplTest {

    @Test
    void mergeRetainsExactNonDominatedCapacity() {
        final SlotCapacityFrontier merged = SlotCapacityFrontierImpl.merge(List.of(
            SlotCapacityFrontierImpl.freeRectangle(8, 8),
            SlotCapacityFrontierImpl.freeRectangle(16, 4),
            SlotCapacityFrontierImpl.freeRectangle(4, 16),
            SlotCapacityFrontierImpl.freeRectangle(2, 2)));

        assertEquals(3, merged.size());
        assertTrue(merged.canFit(8, 8));
        assertTrue(merged.canFit(16, 4));
        assertTrue(merged.canFit(4, 16));
        assertFalse(merged.canFit(9, 9));
        assertFalse(merged.canFit(17, 1));
    }

    @Test
    void occupiedAndInvalidRectanglesNeverFit() {
        assertFalse(SlotCapacityFrontierImpl.empty().canFit(1, 1));
        assertFalse(SlotCapacityFrontierImpl.freeRectangle(0, 8).canFit(1, 1));
    }
}
