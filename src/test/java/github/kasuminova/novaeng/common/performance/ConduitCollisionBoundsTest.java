package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.math.AxisAlignedBB;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConduitCollisionBoundsTest {
    @Test
    void changedArgumentsCannotUseTheComponentCache() {
        final AxisAlignedBB source = new AxisAlignedBB(-0.0, .25, .5, 1, .75, 1);
        assertTrue(ConduitCollisionBounds.matches(source, -0.0, .25, .5, 1, .75, 1));
        assertFalse(ConduitCollisionBounds.matches(source, 0.0, .25, .5, 1, .75, 1));
        assertFalse(ConduitCollisionBounds.matches(source, -0.0, .25, .5, .9, .75, 1));
    }

    @Test
    void fullCubeIsAnOrdinaryImmutableValueAndNegativeZeroIsPreserved() {
        final AxisAlignedBB first = ConduitCollisionBounds.create(0, 0, 0, 1, 1, 1);
        assertEquals(AxisAlignedBB.class, first.getClass());
        assertSame(first, ConduitCollisionBounds.create(0, 0, 0, 1, 1, 1));
        final AxisAlignedBB negative = ConduitCollisionBounds.create(-0.0, 0, 0, 1, 1, 1);
        assertNotSame(first, negative);
        assertBits(new AxisAlignedBB(-0.0, 0, 0, 1, 1, 1), negative);
        final AxisAlignedBB moved = first.offset(2, -3, 4);
        assertBits(new AxisAlignedBB(0, 0, 0, 1, 1, 1), first);
        assertBits(new AxisAlignedBB(2, -3, 4, 3, -2, 5), moved);
    }

    @Test
    void copyingSubclassPreservesConstructorNormalizationAndDoesNotRetainItsDispatch() {
        final AxisAlignedBB subclass = new AxisAlignedBB(0, .25, .5, 1, .75, 1) {
            @Override
            public AxisAlignedBB offset(double x, double y, double z) {
                throw new AssertionError("The original EnderIO copy does not call offset");
            }
        };
        final AxisAlignedBB copied = ConduitCollisionBounds.ordinary(subclass);
        assertEquals(AxisAlignedBB.class, copied.getClass());
        assertBits(subclass, copied);
        copied.offset(1, 2, 3);
    }

    @Test
    void specialAndRandomCoordinatesMatchTheOriginalConstructorBitForBit() {
        final double[] special = {-0.0, 0.0, 1, -1, Double.NaN,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.MIN_VALUE, Double.MAX_VALUE};
        for (final double value : special) {
            assertBits(new AxisAlignedBB(value, 0, 0, 1, 1, 1),
                ConduitCollisionBounds.create(value, 0, 0, 1, 1, 1));
            assertBits(new AxisAlignedBB(0, value, 0, 1, 1, 1),
                ConduitCollisionBounds.create(0, value, 0, 1, 1, 1));
        }
        final Random random = new Random(20261126);
        for (int i = 0; i < 10000; i++) {
            final double a = Double.longBitsToDouble(random.nextLong());
            final double b = Double.longBitsToDouble(random.nextLong());
            final double c = Double.longBitsToDouble(random.nextLong());
            final double d = Double.longBitsToDouble(random.nextLong());
            final double e = Double.longBitsToDouble(random.nextLong());
            final double f = Double.longBitsToDouble(random.nextLong());
            assertBits(new AxisAlignedBB(a, b, c, d, e, f), ConduitCollisionBounds.create(a, b, c, d, e, f));
        }
    }

    private static void assertBits(final AxisAlignedBB expected, final AxisAlignedBB actual) {
        assertEquals(Double.doubleToRawLongBits(expected.minX), Double.doubleToRawLongBits(actual.minX));
        assertEquals(Double.doubleToRawLongBits(expected.minY), Double.doubleToRawLongBits(actual.minY));
        assertEquals(Double.doubleToRawLongBits(expected.minZ), Double.doubleToRawLongBits(actual.minZ));
        assertEquals(Double.doubleToRawLongBits(expected.maxX), Double.doubleToRawLongBits(actual.maxX));
        assertEquals(Double.doubleToRawLongBits(expected.maxY), Double.doubleToRawLongBits(actual.maxY));
        assertEquals(Double.doubleToRawLongBits(expected.maxZ), Double.doubleToRawLongBits(actual.maxZ));
    }
}
