package github.kasuminova.novaeng.mixin.mmce;

import hellfirepvp.modularmachinery.client.util.DynamicMachineRenderContext;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DynamicMachineRenderContext.class, remap = false)
public class MixinDynamicMachineRenderContext {

    @Inject(method = "addControllerToBlockArray", at = @At("HEAD"), cancellable = true)
    private static void addControllerToBlockArray(DynamicMachine machine, BlockArray copy, Vec3i moveOffset, CallbackInfo ci) {
        if (!"modularmachinery".equals(machine.getRegistryName().getNamespace())) {
            ci.cancel();
        }
    }

}