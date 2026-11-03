package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class CollisionBoxQueries {
    private CollisionBoxQueries() {
    }

    public static void append(final BlockPos pos, final AxisAlignedBB query,
                              final List<AxisAlignedBB> result, final AxisAlignedBB shape) {
        if (shape == null) return;
        // Mod-provided bounding boxes may override offset/intersects.
        if (shape.getClass() != AxisAlignedBB.class || query.getClass() != AxisAlignedBB.class) {
            final AxisAlignedBB moved = shape.offset(pos);
            if (query.intersects(moved)) result.add(moved);
            return;
        }
        final double minX = shape.minX + pos.getX();
        final double maxX = shape.maxX + pos.getX();
        if (!(query.minX < maxX && query.maxX > minX)) return;
        final double minY = shape.minY + pos.getY();
        final double maxY = shape.maxY + pos.getY();
        if (!(query.minY < maxY && query.maxY > minY)) return;
        final double minZ = shape.minZ + pos.getZ();
        final double maxZ = shape.maxZ + pos.getZ();
        if (query.minZ < maxZ && query.maxZ > minZ) {
            result.add(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ));
        }
    }
}
