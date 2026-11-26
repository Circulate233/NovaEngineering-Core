package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.math.BlockPos;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

/** Ordered read-only scan views used only by DE's guarded injector-scan expression patch. */
public final class FusionInjectorPositions {
    private FusionInjectorPositions() {
    }

    public static List<BlockPos> scan(final BlockPos center, final int radius) {
        return new Cross(center, radius);
    }

    public static List<BlockPos> view(final Iterable<BlockPos> positions) {
        // The original line-of-sight list is used only by foreach. Preserve immutable positions.
        return new AbstractList<>() {
            @Override public Iterator<BlockPos> iterator() { return positions.iterator(); }
            @Override public BlockPos get(final int index) {
                if (index < 0) throw new IndexOutOfBoundsException(index);
                int i = 0;
                for (final BlockPos position : positions) if (i++ == index) return position;
                throw new IndexOutOfBoundsException(index);
            }
            @Override public int size() {
                int count = 0;
                for (final BlockPos ignored : positions) count++;
                return count;
            }
        };
    }

    private static final class Cross extends AbstractList<BlockPos> implements RandomAccess {
        private final int x;
        private final int y;
        private final int z;
        private final int radius;
        private final int length;
        private final int sectionSize;

        private Cross(final BlockPos center, final int radius) {
            if (radius < 0) throw new IllegalArgumentException("Negative scan radius");
            x = center.getX();
            y = center.getY();
            z = center.getZ();
            this.radius = radius;
            length = Math.addExact(Math.multiplyExact(radius, 2), 1);
            sectionSize = Math.multiplyExact(length, 9);
            Math.multiplyExact(sectionSize, 3);
        }

        @Override public int size() { return sectionSize * 3; }

        @Override public BlockPos get(final int index) {
            if (index < 0 || index >= size()) throw new IndexOutOfBoundsException(index);
            final int axis = index / sectionSize;
            final int offset = index % sectionSize;
            final int width = axis == 0 ? length : 3;
            final int height = axis == 1 ? length : 3;
            // BlockPos.getAllInBox order: x first, then y, then z. Intersections stay duplicated.
            return new BlockPos(x - (axis == 0 ? radius : 1) + offset % width,
                y - (axis == 1 ? radius : 1) + offset / width % height,
                z - (axis == 2 ? radius : 1) + offset / (width * height));
        }
    }
}
