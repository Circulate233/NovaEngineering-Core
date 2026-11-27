package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.math.BlockPos;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FusionInjectorPositionsTest {
    @Test
    void crossMatchesOriginalThreeBoxesIncludingDuplicatesAndOrder() {
        final List<BlockPos> centers = List.of(BlockPos.ORIGIN, new BlockPos(-73, 61, 117),
            new BlockPos(29999983, 254, -29999983));
        for (int centerIndex = 0; centerIndex < centers.size(); centerIndex++) {
            final BlockPos center = centers.get(centerIndex);
            for (final int radius : new int[]{0, 1, 2, 16}) {
                final List<BlockPos> expected = new ObjectArrayList<>();
                for (int axis = 0; axis < 3; axis++) {
                    final int x = axis == 0 ? radius : 1;
                    final int y = axis == 1 ? radius : 1;
                    final int z = axis == 2 ? radius : 1;
                    for (final BlockPos pos : BlockPos.getAllInBox(center.add(-x, -y, -z), center.add(x, y, z))) expected.add(pos);
                }
                final List<BlockPos> actual = FusionInjectorPositions.scan(center, radius);
                assertEquals(expected, actual);
                assertNotSame(actual.get(0), actual.get(0));
                assertThrows(IndexOutOfBoundsException.class, () -> actual.get(-1));
                assertThrows(IndexOutOfBoundsException.class, () -> actual.get(actual.size()));
            }
        }
    }

    @Test
    void centerAndReturnedPositionsAreStableWhileLineViewRemainsLazy() {
        final BlockPos.MutableBlockPos center = new BlockPos.MutableBlockPos(5, 7, 9);
        final List<BlockPos> positions = FusionInjectorPositions.scan(center, 16);
        final BlockPos first = positions.get(0);
        center.setPos(500, 700, 900);
        assertEquals(first, positions.get(0));
        positions.get(200);
        assertEquals(new BlockPos(-11, 6, 8), first);
        final AtomicInteger iterations = new AtomicInteger();
        final List<BlockPos> line = List.of(first, first, new BlockPos(1, 2, 3));
        final List<BlockPos> view = FusionInjectorPositions.view(() -> {
            iterations.incrementAndGet();
            return line.iterator();
        });
        assertEquals(0, iterations.get());
        assertEquals(line, view);
        assertEquals(line, view);
        assertThrows(UnsupportedOperationException.class, () -> view.add(first));
    }
}
