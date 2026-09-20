package github.kasuminova.novaeng.mixin.biomesoplenty;

import biomesoplenty.common.remote.TrailManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips BOP's synchronous remote trail download while keeping TrailManager's
 * statically initialized trail maps empty.
 */
@Mixin(value = TrailManager.class, remap = false)
public abstract class MixinTrailManager {

    @Inject(method = "retrieveTrails", at = @At("HEAD"), cancellable = true, require = 1)
    private static void nova$skipRemoteTrailRetrieval(final CallbackInfo ci) {
        ci.cancel();
    }
}
