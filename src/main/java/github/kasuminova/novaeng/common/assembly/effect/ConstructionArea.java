package github.kasuminova.novaeng.common.assembly.effect;

import net.minecraft.util.math.BlockPos;

public record ConstructionArea(BlockPos min, BlockPos max) {
    private static final int RENDER_DISTANCE = 32;

    public ConstructionArea {
        min = min.toImmutable();
        max = max.toImmutable();
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ())
            throw new IllegalArgumentException("Invalid construction area");
    }

    private static double outside(double coordinate, int min, int max) {
        return Math.max(0, Math.max(min - coordinate, coordinate - (max + 1.0)));
    }

    public boolean withinRenderDistance(double x, double y, double z) {
        double dx = outside(x, min.getX(), max.getX());
        double dy = outside(y, min.getY(), max.getY());
        double dz = outside(z, min.getZ(), max.getZ());
        return dx * dx + dy * dy + dz * dz <= RENDER_DISTANCE * RENDER_DISTANCE;
    }

    static final class Builder {
        private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        void include(int x, int y, int z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        ConstructionArea build() {
            return new ConstructionArea(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
        }
    }
}
