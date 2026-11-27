package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.EnumFacing;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProcessorDirectionsTest {
    @Test
    void allMasksKeepVanillaOrderAndRemainIndependentMutableSnapshots() {
        for (int mask = 0; mask < 64; mask++) {
            final Object[] handlers = new Object[6];
            final List<EnumFacing> expected = new ObjectArrayList<>();
            for (int i = 0; i < 6; i++) if ((mask & (1 << i)) != 0) {
                handlers[i] = new Object();
                expected.add(EnumFacing.values()[i]);
            }
            final List<EnumFacing> actual = ProcessorDirections.snapshot(handlers);
            final List<EnumFacing> sibling = ProcessorDirections.snapshot(handlers);
            assertEquals(expected, actual);
            // An override may reorder, add nulls, remove through an iterator, or retain the list.
            actual.add(null);
            actual.add(0, EnumFacing.UP);
            actual.set(0, EnumFacing.DOWN);
            Collections.reverse(actual);
            actual.removeIf(value -> value == null);
            actual.clear();
            assertEquals(expected, sibling);
            assertEquals(expected, ProcessorDirections.snapshot(handlers));
            java.util.Arrays.fill(handlers, null);
            assertEquals(expected, sibling);
        }
    }

    @Test
    void malformedShortHandlerArraysKeepOriginalFailure() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ProcessorDirections.snapshot(new Object[5]));
    }
}
