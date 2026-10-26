package github.kasuminova.novaeng.common.phys;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/**
 * Allocation-free candidate selection for the vanilla 1.12.2 AABB ray trace.
 *
 * <p>The returned hit still owns the one {@link Vec3d} required by
 * {@link RayTraceResult}. The six temporary points used by vanilla while testing the faces stay
 * in primitive locals. Keeping this code outside the mixin also makes the arithmetic testable
 * without loading Mixin infrastructure.</p>
 */
public final class NovaAabbIntercept {
    private static final double AXIS_EPSILON = 1.0E-7D;

    private NovaAabbIntercept() {
    }

    public static RayTraceResult calculate(final double minX, final double minY, final double minZ,
                                           final double maxX, final double maxY, final double maxZ,
                                           final Vec3d start, final Vec3d end) {
        final double startX = start.x;
        final double startY = start.y;
        final double startZ = start.z;
        final double deltaX = end.x - startX;
        final double deltaY = end.y - startY;
        final double deltaZ = end.z - startZ;

        boolean found = false;
        double hitX = 0.0D;
        double hitY = 0.0D;
        double hitZ = 0.0D;
        double hitDistance = 0.0D;
        EnumFacing hitSide = null;

        if (deltaX * deltaX >= AXIS_EPSILON) {
            final double t = (minX - startX) / deltaX;
            if (t >= 0.0D && t <= 1.0D) {
                final double candidateY = startY + deltaY * t;
                final double candidateZ = startZ + deltaZ * t;
                if (candidateY >= minY && candidateY <= maxY
                    && candidateZ >= minZ && candidateZ <= maxZ) {
                    hitX = startX + deltaX * t;
                    hitY = candidateY;
                    hitZ = candidateZ;
                    hitDistance = distanceSquared(startX, startY, startZ, hitX, hitY, hitZ);
                    hitSide = EnumFacing.WEST;
                    found = true;
                }
            }

            final double eastT = (maxX - startX) / deltaX;
            if (eastT >= 0.0D && eastT <= 1.0D) {
                final double candidateY = startY + deltaY * eastT;
                final double candidateZ = startZ + deltaZ * eastT;
                if (candidateY >= minY && candidateY <= maxY
                    && candidateZ >= minZ && candidateZ <= maxZ) {
                    final double candidateX = startX + deltaX * eastT;
                    final double candidateDistance = distanceSquared(
                        startX, startY, startZ, candidateX, candidateY, candidateZ);
                    if (!found || candidateDistance < hitDistance) {
                        hitX = candidateX;
                        hitY = candidateY;
                        hitZ = candidateZ;
                        hitDistance = candidateDistance;
                        hitSide = EnumFacing.EAST;
                        found = true;
                    }
                }
            }
        }

        if (deltaY * deltaY >= AXIS_EPSILON) {
            final double t = (minY - startY) / deltaY;
            if (t >= 0.0D && t <= 1.0D) {
                final double candidateX = startX + deltaX * t;
                final double candidateZ = startZ + deltaZ * t;
                if (candidateX >= minX && candidateX <= maxX
                    && candidateZ >= minZ && candidateZ <= maxZ) {
                    final double candidateY = startY + deltaY * t;
                    final double candidateDistance = distanceSquared(
                        startX, startY, startZ, candidateX, candidateY, candidateZ);
                    if (!found || candidateDistance < hitDistance) {
                        hitX = candidateX;
                        hitY = candidateY;
                        hitZ = candidateZ;
                        hitDistance = candidateDistance;
                        hitSide = EnumFacing.DOWN;
                        found = true;
                    }
                }
            }

            final double upT = (maxY - startY) / deltaY;
            if (upT >= 0.0D && upT <= 1.0D) {
                final double candidateX = startX + deltaX * upT;
                final double candidateZ = startZ + deltaZ * upT;
                if (candidateX >= minX && candidateX <= maxX
                    && candidateZ >= minZ && candidateZ <= maxZ) {
                    final double candidateY = startY + deltaY * upT;
                    final double candidateDistance = distanceSquared(
                        startX, startY, startZ, candidateX, candidateY, candidateZ);
                    if (!found || candidateDistance < hitDistance) {
                        hitX = candidateX;
                        hitY = candidateY;
                        hitZ = candidateZ;
                        hitDistance = candidateDistance;
                        hitSide = EnumFacing.UP;
                        found = true;
                    }
                }
            }
        }

        if (deltaZ * deltaZ >= AXIS_EPSILON) {
            final double t = (minZ - startZ) / deltaZ;
            if (t >= 0.0D && t <= 1.0D) {
                final double candidateX = startX + deltaX * t;
                final double candidateY = startY + deltaY * t;
                if (candidateX >= minX && candidateX <= maxX
                    && candidateY >= minY && candidateY <= maxY) {
                    final double candidateZ = startZ + deltaZ * t;
                    final double candidateDistance = distanceSquared(
                        startX, startY, startZ, candidateX, candidateY, candidateZ);
                    if (!found || candidateDistance < hitDistance) {
                        hitX = candidateX;
                        hitY = candidateY;
                        hitZ = candidateZ;
                        hitDistance = candidateDistance;
                        hitSide = EnumFacing.NORTH;
                        found = true;
                    }
                }
            }

            final double southT = (maxZ - startZ) / deltaZ;
            if (southT >= 0.0D && southT <= 1.0D) {
                final double candidateX = startX + deltaX * southT;
                final double candidateY = startY + deltaY * southT;
                if (candidateX >= minX && candidateX <= maxX
                    && candidateY >= minY && candidateY <= maxY) {
                    final double candidateZ = startZ + deltaZ * southT;
                    final double candidateDistance = distanceSquared(
                        startX, startY, startZ, candidateX, candidateY, candidateZ);
                    if (!found || candidateDistance < hitDistance) {
                        hitX = candidateX;
                        hitY = candidateY;
                        hitZ = candidateZ;
                        hitSide = EnumFacing.SOUTH;
                        found = true;
                    }
                }
            }
        }

        return found ? new RayTraceResult(new Vec3d(hitX, hitY, hitZ), hitSide) : null;
    }

    private static double distanceSquared(final double startX, final double startY, final double startZ,
                                          final double x, final double y, final double z) {
        final double dx = x - startX;
        final double dy = y - startY;
        final double dz = z - startZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
