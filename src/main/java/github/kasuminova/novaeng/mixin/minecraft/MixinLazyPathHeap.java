package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.pathfinding.PathHeap;
import net.minecraft.pathfinding.PathPoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathHeap.class)
public abstract class MixinLazyPathHeap {
    @Shadow
    private PathPoint[] pathPoints;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 128), require = 1)
    private int nova$deferHeapArray(final int capacity) {
        return 0;
    }

    @Inject(method = "addPoint", at = @At("HEAD"), require = 1)
    private void nova$allocateHeapArray(final PathPoint point, final CallbackInfoReturnable<PathPoint> cir) {
        // Keep vanilla's duplicate-assignment failure ahead of backing allocation.
        if (point.index < 0 && this.pathPoints.length == 0) {
            this.pathPoints = new PathPoint[128];
        }
    }
}
