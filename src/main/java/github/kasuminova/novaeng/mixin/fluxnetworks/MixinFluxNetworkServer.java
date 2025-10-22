package github.kasuminova.novaeng.mixin.fluxnetworks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sonar.fluxnetworks.api.network.ITransferHandler;
import sonar.fluxnetworks.common.connection.FluxNetworkBase;
import sonar.fluxnetworks.common.connection.FluxNetworkServer;

@Mixin(value = FluxNetworkServer.class, remap = false)
public abstract class MixinFluxNetworkServer extends FluxNetworkBase {

    @Shadow
    public long bufferLimiter;

    @Redirect(method = "onEndServerTick", at = @At(value = "INVOKE", target = "Lsonar/fluxnetworks/api/network/ITransferHandler;getRequest()J", ordinal = 1))
    public long getRequest(ITransferHandler instance) {
        long i = instance.getRequest();
        if (this.bufferLimiter == Long.MAX_VALUE) {
            return 0;
        } else return Math.min(Long.MAX_VALUE - this.bufferLimiter, i);
    }
}
