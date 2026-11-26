package github.kasuminova.novaeng.mixin.enderio;

import com.enderio.core.client.render.BoundingBox;
import crazypants.enderio.base.conduit.geom.CollidableComponent;
import github.kasuminova.novaeng.common.performance.ConduitCollisionBounds;
import github.kasuminova.novaeng.common.performance.ConduitComponentBounds;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = CollidableComponent.class, remap = false)
public abstract class MixinCollidableComponent implements ConduitComponentBounds {
    @Shadow @Final public BoundingBox bound;

    @Unique private volatile AxisAlignedBB nova$ordinaryBounds;

    @Override
    public AxisAlignedBB nova$ordinaryCollisionBounds() {
        AxisAlignedBB box = nova$ordinaryBounds;
        if (box == null) {
            // The component's bound reference and its coordinates are final. Rebuilt geometry
            // gets a new component, so the cached value has the same lifetime, without a map.
            box = ConduitCollisionBounds.ordinary(bound);
            nova$ordinaryBounds = box;
        }
        return box;
    }
}
