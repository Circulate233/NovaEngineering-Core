package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.client.gui.widget.impl.preview.WorldSceneRendererWidget;
import github.kasuminova.novaeng.common.util.NEWDynamicMachine;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorldSceneRendererWidget.class, remap = false)
public class MixinWorldSceneRendererWidget {

    @Shadow
    protected BlockArray pattern;

    @Inject(method = "addControllerToPattern", at = @At("HEAD"), cancellable = true)
    protected void addControllerToPattern(DynamicMachine machine, CallbackInfo ci) {
        if (machine instanceof NEWDynamicMachine n) {
            ci.cancel();
        }
    }
}