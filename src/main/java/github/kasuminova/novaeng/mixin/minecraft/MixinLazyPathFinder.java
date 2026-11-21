package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PathFinder.class)
public abstract class MixinLazyPathFinder {
    @Shadow
    @Final
    @Mutable
    private PathPoint[] pathOptions;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 32), require = 1)
    private int nova$deferOptionsArray(final int capacity) {
        return 0;
    }

    @Inject(method = "findPath(Lnet/minecraft/pathfinding/PathPoint;Lnet/minecraft/pathfinding/PathPoint;F)Lnet/minecraft/pathfinding/Path;",
        at = @At("HEAD"), require = 1)
    private void nova$allocateOptionsArray(final PathPoint from, final PathPoint to, final float maxDistance,
                                           final CallbackInfoReturnable<Path> cir) {
        if (this.pathOptions.length == 0) {
            this.pathOptions = new PathPoint[32];
        }
    }
}
