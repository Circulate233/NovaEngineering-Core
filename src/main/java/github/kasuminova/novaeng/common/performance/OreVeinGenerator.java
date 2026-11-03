package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.math.MathHelper;

import java.util.Arrays;
import java.util.random.RandomGenerator;

public final class OreVeinGenerator {
    private OreVeinGenerator() {
    }

    public static boolean generate(final int x, final int y, final int z, final int size, final boolean tiny,
                                   final RandomGenerator random, final Target target, final Scratch scratch) {
        if (size <= 8) return generateSmall(x, y, z, size, tiny, random, target);
        scratch.begin(x, y, z, size);
        final int minTargetX = target.minX();
        final int maxTargetX = target.maxX();
        final int minTargetZ = target.minZ();
        final int maxTargetZ = target.maxZ();
        final float angle = random.nextFloat() * (float) Math.PI;
        final double deltaX = MathHelper.sin(angle) * size / 8.0F;
        final double deltaZ = MathHelper.cos(angle) * size / 8.0F;
        final double startY = y + random.nextInt(3) - 2;
        final double endY = y + random.nextInt(3) - 2;
        final int group = size >= 24 ? 4 : size >= 12 ? 3 : 2;
        for (int step = 0; step <= size; step += group) {
            double radius = -1.0;
            double progress = 0.0;
            for (int offset = 0; offset < group && step + offset <= size; offset++) {
                final double candidateProgress = (double) (step + offset) / size;
                final double amplitude = random.nextDouble() * size / 16.0;
                final double candidateRadius = ((MathHelper.sin((float) (Math.PI * candidateProgress)) + 1.0) * amplitude + 1.0) * 0.5;
                if (candidateRadius > radius) {
                    radius = candidateRadius;
                    progress = candidateProgress;
                }
            }
            final double centerX = x + deltaX * (1.0 - 2.0 * progress);
            final double centerY = startY + (endY - startY) * progress;
            final double centerZ = z + deltaZ * (1.0 - 2.0 * progress);
            final int minX = Math.max(minTargetX, MathHelper.floor(centerX - radius));
            final int maxX = Math.min(maxTargetX, MathHelper.floor(centerX + radius));
            final int minZ = Math.max(minTargetZ, MathHelper.floor(centerZ - radius));
            final int maxZ = Math.min(maxTargetZ, MathHelper.floor(centerZ + radius));
            if (minX > maxX || minZ > maxZ) continue;
            final double inverse = 1.0 / radius;
            for (int blockX = minX; blockX <= maxX; blockX++) {
                final double dx = (blockX + 0.5 - centerX) * inverse;
                final double xx = dx * dx;
                if (xx >= 1.0) continue;
                for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                    final double dz = (blockZ + 0.5 - centerZ) * inverse;
                    final double xz = xx + dz * dz;
                    if (xz >= 1.0) continue;
                    final double reachY = radius * Math.sqrt(1.0 - xz);
                    final int minY = MathHelper.floor(centerY - reachY - 0.5) + 1;
                    final int maxY = MathHelper.ceil(centerY + reachY - 0.5) - 1;
                    if (minY > maxY) continue;
                    final int column = (blockX - scratch.x) << scratch.axisShift | blockZ - scratch.z;
                    final int start = minY - scratch.y;
                    final int end = maxY - scratch.y;
                    final long mask = ((1L << (end - start + 1)) - 1L) << start;
                    scratch.columns[column] |= (int) mask;
                    scratch.firstColumn = Math.min(scratch.firstColumn, column);
                    scratch.lastColumn = Math.max(scratch.lastColumn, column);
                }
            }
        }
        boolean generated = false;
        for (int column = scratch.firstColumn; column <= scratch.lastColumn; column++) {
            int values = scratch.columns[column];
            while (values != 0) {
                final int offsetY = Integer.numberOfTrailingZeros(values);
                values &= values - 1;
                generated |= target.place(scratch.x + (column >>> scratch.axisShift), scratch.y + offsetY,
                    scratch.z + (column & scratch.axisMask));
            }
        }
        return generated;
    }

    private static boolean generateSmall(final int x, final int y, final int z, final int size, final boolean tiny,
                                         final RandomGenerator random, final Target target) {
        final int minTargetX = target.minX();
        final int maxTargetX = target.maxX();
        final int minTargetZ = target.minZ();
        final int maxTargetZ = target.maxZ();
        long visited = 0L;
        boolean generated = false;
        if (tiny && size < 4) {
            for (int step = 0; step < size; step++) {
                final int dx = step == 0 ? 0 : random.nextInt(2);
                final int dy = step == 0 ? 0 : random.nextInt(2);
                final int dz = step == 0 ? 0 : random.nextInt(2);
                final int blockX = x + dx;
                final int blockZ = z + dz;
                if (blockX < minTargetX || blockX > maxTargetX || blockZ < minTargetZ || blockZ > maxTargetZ) continue;
                final long bit = 1L << (dx << 2 | dy << 1 | dz);
                if ((visited & bit) != 0L) continue;
                visited |= bit;
            }
            while (visited != 0L) {
                final int index = Long.numberOfTrailingZeros(visited);
                visited &= visited - 1L;
                generated |= target.place(x + (index >>> 2), y + (index >>> 1 & 1), z + (index & 1));
            }
            return generated;
        }
        final float angle = random.nextFloat() * (float) Math.PI;
        final double deltaX = MathHelper.sin(angle) * size / 8.0F;
        final double deltaZ = MathHelper.cos(angle) * size / 8.0F;
        final double startY = y + random.nextInt(3) - 2;
        final double endY = y + random.nextInt(3) - 2;
        final int group = size < 8 ? 1 : 2;
        for (int step = 0; step <= size; step += group) {
            double radius = -1.0;
            double progress = 0.0;
            for (int offset = 0; offset < group && step + offset <= size; offset++) {
                final double candidateProgress = (double) (step + offset) / size;
                final double amplitude = random.nextDouble() * size / 16.0;
                final double candidateRadius = ((MathHelper.sin((float) (Math.PI * candidateProgress)) + 1.0) * amplitude + 1.0) * 0.5;
                if (candidateRadius > radius) {
                    radius = candidateRadius;
                    progress = candidateProgress;
                }
            }
            final double centerX = x + deltaX * (1.0 - 2.0 * progress);
            final double centerY = startY + (endY - startY) * progress;
            final double centerZ = z + deltaZ * (1.0 - 2.0 * progress);
            final int minX = Math.max(minTargetX, MathHelper.floor(centerX - radius));
            final int maxX = Math.min(maxTargetX, MathHelper.floor(centerX + radius));
            final int minY = MathHelper.floor(centerY - radius);
            final int maxY = MathHelper.floor(centerY + radius);
            final int minZ = Math.max(minTargetZ, MathHelper.floor(centerZ - radius));
            final int maxZ = Math.min(maxTargetZ, MathHelper.floor(centerZ + radius));
            final double inverse = 1.0 / radius;
            for (int blockX = minX; blockX <= maxX; blockX++) {
                final double dx = (blockX + 0.5 - centerX) * inverse;
                final double xx = dx * dx;
                if (xx >= 1.0) continue;
                for (int blockY = minY; blockY <= maxY; blockY++) {
                    final double dy = (blockY + 0.5 - centerY) * inverse;
                    final double xy = xx + dy * dy;
                    if (xy >= 1.0) continue;
                    for (int blockZ = minZ; blockZ <= maxZ; blockZ++) {
                        final double dz = (blockZ + 0.5 - centerZ) * inverse;
                        if (xy + dz * dz >= 1.0) continue;
                        final long bit = 1L << ((blockX - x + 2) << 4 | (blockY - y + 3) << 2 | blockZ - z + 2);
                        if ((visited & bit) != 0L) continue;
                        visited |= bit;
                    }
                }
            }
        }
        while (visited != 0L) {
            final int index = Long.numberOfTrailingZeros(visited);
            visited &= visited - 1L;
            generated |= target.place(x + (index >>> 4) - 2, y + (index >>> 2 & 3) - 3, z + (index & 3) - 2);
        }
        return generated;
    }

    public interface Target {
        default int minX() {
            return Integer.MIN_VALUE;
        }

        default int maxX() {
            return Integer.MAX_VALUE;
        }

        default int minZ() {
            return Integer.MIN_VALUE;
        }

        default int maxZ() {
            return Integer.MAX_VALUE;
        }

        boolean place(int x, int y, int z);
    }

    public static final class Scratch {
        private final int[] columns = new int[1024];
        private int x;
        private int y;
        private int z;
        private int axisShift;
        private int axisMask;
        private int firstColumn = 1024;
        private int lastColumn = -1;

        private void begin(final int x, final int y, final int z, final int size) {
            if (this.firstColumn <= this.lastColumn)
                Arrays.fill(this.columns, this.firstColumn, this.lastColumn + 1, 0);
            final int reach = (size * 3 + 15) / 16 + 1;
            this.axisShift = 32 - Integer.numberOfLeadingZeros(reach * 2);
            this.axisMask = (1 << this.axisShift) - 1;
            this.x = x - reach;
            this.y = y - 8;
            this.z = z - reach;
            this.firstColumn = 1024;
            this.lastColumn = -1;
        }

    }
}
