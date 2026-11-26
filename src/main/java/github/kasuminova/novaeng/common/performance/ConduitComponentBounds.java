package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.math.AxisAlignedBB;

/** One ordinary immutable box owned by the lifetime of a conduit component's final geometry. */
public interface ConduitComponentBounds {
    AxisAlignedBB nova$ordinaryCollisionBounds();
}
