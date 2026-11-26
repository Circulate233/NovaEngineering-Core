package github.kasuminova.novaeng.mixin.enderio;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import crazypants.enderio.base.conduit.geom.CollidableComponent;
import crazypants.enderio.conduits.conduit.BlockConduitBundle;
import github.kasuminova.novaeng.common.performance.ConduitCollisionBounds;
import github.kasuminova.novaeng.common.performance.ConduitComponentBounds;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockConduitBundle.class, remap = false)
public abstract class MixinBlockConduitBundleBounds {
    @Shadow private AxisAlignedBB bounds;

    @WrapOperation(method = "addCollisionBoxToList", remap = true,
        at = @At(value = "INVOKE", remap = false, ordinal = 1,
            target = "Lcrazypants/enderio/conduits/conduit/BlockConduitBundle;setBlockBounds(DDDDDD)Lnet/minecraft/util/math/AxisAlignedBB;"),
        require = 1, allow = 1)
    private AxisAlignedBB nova$componentBounds(final BlockConduitBundle block,
                                              final double minX, final double minY, final double minZ,
                                              final double maxX, final double maxY, final double maxZ,
                                              final Operation<AxisAlignedBB> original,
                                              @Local final CollidableComponent component) {
        if (block == (Object) this && component instanceof ConduitComponentBounds cached
            && ConduitCollisionBounds.matches(component.bound, minX, minY, minZ, maxX, maxY, maxZ)) {
            // Preserve the original bounds assignment before the unmodified superclass
            // collision callback. Do not install BoundingBox (a subclass) in this field.
            return bounds = cached.nova$ordinaryCollisionBounds();
        }
        return original.call(block, minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Redirect(method = "setBlockBounds", at = @At(value = "NEW",
        target = "net/minecraft/util/math/AxisAlignedBB", remap = true), require = 1, allow = 1)
    private AxisAlignedBB nova$reuseFullCube(final double minX, final double minY, final double minZ,
                                            final double maxX, final double maxY, final double maxZ) {
        // The method's original PUTFIELD/return remain intact, including its final reset.
        return ConduitCollisionBounds.create(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
