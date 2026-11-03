package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.client.renderer.ControllerModelRenderManager;
import github.kasuminova.mmce.client.renderer.GeoModelRenderTask;
import github.kasuminova.novaeng.client.memory.MmceRenderMemory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ControllerModelRenderManager.class, remap = false)
public abstract class MixinControllerModelTaskRetirement {
    @Redirect(method = "checkControllerState", at = @At(value = "INVOKE",
        target = "Lgithub/kasuminova/mmce/client/renderer/GeoModelRenderTask;finalize()V"), require = 1)
    private void nova$retireCompletedModel(final GeoModelRenderTask task) {
        MmceRenderMemory.retire(task);
    }
}
