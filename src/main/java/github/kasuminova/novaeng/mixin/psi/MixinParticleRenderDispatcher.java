package github.kasuminova.novaeng.mixin.psi;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.psi.client.fx.ParticleRenderDispatcher;
import vazkii.psi.client.fx.FXSparkle;
import vazkii.psi.client.fx.FXWisp;

@Mixin(value = ParticleRenderDispatcher.class, remap = false)
public class MixinParticleRenderDispatcher {

    @Inject(method = "onRenderWorldLast", at = @At("HEAD"), cancellable = true)
    private static void nova$skipWhenNoParticles(final RenderWorldLastEvent event, final CallbackInfo ci) {
        if (!FXSparkle.queuedRenders.isEmpty()
            || !FXWisp.queuedRenders.isEmpty()
            || !FXWisp.queuedDepthIgnoringRenders.isEmpty()) {
            return;
        }
        ParticleRenderDispatcher.wispFxCount = 0;
        ParticleRenderDispatcher.depthIgnoringWispFxCount = 0;
        ParticleRenderDispatcher.sparkleFxCount = 0;
        ci.cancel();
    }
}
