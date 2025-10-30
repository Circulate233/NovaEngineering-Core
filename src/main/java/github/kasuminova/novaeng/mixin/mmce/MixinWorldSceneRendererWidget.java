package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.client.gui.widget.impl.preview.WorldSceneRendererWidget;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorldSceneRendererWidget.class, remap = false)
public class MixinWorldSceneRendererWidget {

    @Inject(method = "addControllerToPattern", at = @At("HEAD"), cancellable = true)
    protected void addControllerToPattern(DynamicMachine machine, CallbackInfo ci) {
        if (!"modularmachinery".equals(machine.getRegistryName().getNamespace())) {
            ci.cancel();
        }
    }
}