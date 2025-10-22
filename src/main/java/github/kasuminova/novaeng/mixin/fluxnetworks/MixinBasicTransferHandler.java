package github.kasuminova.novaeng.mixin.fluxnetworks;

import github.kasuminova.novaeng.common.handler.DreamEnergyPortHandler;
import github.kasuminova.novaeng.common.machine.DreamEnergyCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sonar.fluxnetworks.common.connection.transfer.BasicTransferHandler;

import java.math.BigInteger;

import static github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils.BigLongMax;

@Mixin(value = BasicTransferHandler.class, remap = false)
public class MixinBasicTransferHandler {

    @Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true)
    public final void getBufferMixin(CallbackInfoReturnable<Long> cir) {
        if ((Object) this instanceof DreamEnergyPortHandler handler) {
            var ctrl = handler.getCtrl();
            if (ctrl != null) {
                BigInteger energy = DreamEnergyCore.getEnergyStored(ctrl);
                if (energy.compareTo(BigLongMax) >= 0) {
                    cir.setReturnValue(Long.MAX_VALUE);
                } else {
                    cir.setReturnValue(energy.longValue());
                }
            }
        }
    }
}
