package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.math.AxisAlignedBB;

/** Immutable local-space boxes only. Never stores translated query results. */
public final class ConduitCollisionBounds {
    private static final AxisAlignedBB FULL_CUBE = new AxisAlignedBB(0, 0, 0, 1, 1, 1);

    private ConduitCollisionBounds() {
    }

    public static AxisAlignedBB ordinary(final AxisAlignedBB shape) {
        return create(shape.minX, shape.minY, shape.minZ, shape.maxX, shape.maxY, shape.maxZ);
    }

    public static boolean matches(final AxisAlignedBB shape, final double minX, final double minY,
                                   final double minZ, final double maxX, final double maxY, final double maxZ) {
        return Double.doubleToRawLongBits(shape.minX) == Double.doubleToRawLongBits(minX)
            && Double.doubleToRawLongBits(shape.minY) == Double.doubleToRawLongBits(minY)
            && Double.doubleToRawLongBits(shape.minZ) == Double.doubleToRawLongBits(minZ)
            && Double.doubleToRawLongBits(shape.maxX) == Double.doubleToRawLongBits(maxX)
            && Double.doubleToRawLongBits(shape.maxY) == Double.doubleToRawLongBits(maxY)
            && Double.doubleToRawLongBits(shape.maxZ) == Double.doubleToRawLongBits(maxZ);
    }

    public static AxisAlignedBB create(final double minX, final double minY, final double minZ,
                                       final double maxX, final double maxY, final double maxZ) {
        // == alone would also canonicalize negative zero, unlike the original constructor.
        if (Double.doubleToRawLongBits(minX) == 0 && Double.doubleToRawLongBits(minY) == 0
            && Double.doubleToRawLongBits(minZ) == 0 && maxX == 1 && maxY == 1 && maxZ == 1) {
            return FULL_CUBE;
        }
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
