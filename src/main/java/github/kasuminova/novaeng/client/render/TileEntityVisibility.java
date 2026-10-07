package github.kasuminova.novaeng.client.render;

/**
 * Conservative bounds checks shared by the TESR distance and frustum gates.
 */
public final class TileEntityVisibility {
    private static final double MAX_DISTANCE_GATED_EXTENT = 32.0D;

    private TileEntityVisibility() {
    }

    public static boolean hasFiniteBounds(final double minX, final double minY, final double minZ,
                                          final double maxX, final double maxY, final double maxZ) {
        return Double.isFinite(minX) && Double.isFinite(minY) && Double.isFinite(minZ)
            && Double.isFinite(maxX) && Double.isFinite(maxY) && Double.isFinite(maxZ)
            && minX <= maxX && minY <= maxY && minZ <= maxZ;
    }

    /**
     * Large structures are exempt; distance is measured to the whole volume, not its block position.
     */
    public static boolean isBeyondDistance(final double minX, final double minY, final double minZ,
                                           final double maxX, final double maxY, final double maxZ,
                                           final double x, final double y, final double z, final int limit) {
        if (limit <= 0 || !hasFiniteBounds(minX, minY, minZ, maxX, maxY, maxZ)
            || maxX - minX > MAX_DISTANCE_GATED_EXTENT
            || maxY - minY > MAX_DISTANCE_GATED_EXTENT
            || maxZ - minZ > MAX_DISTANCE_GATED_EXTENT) {
            return false;
        }
        final double dx = axisDistance(x, minX, maxX);
        final double dy = axisDistance(y, minY, maxY);
        final double dz = axisDistance(z, minZ, maxZ);
        return dx * dx + dy * dy + dz * dz > (double) limit * limit;
    }

    private static double axisDistance(final double value, final double min, final double max) {
        return value < min ? min - value : Math.max(value - max, 0.0D);
    }
}
