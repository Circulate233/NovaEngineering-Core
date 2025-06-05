package github.kasuminova.novaeng.mixin.fluxnetworks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.kasuminova.novaeng.common.handler.DreamEnergyPortHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sonar.fluxnetworks.api.network.FluxLogicType;
import sonar.fluxnetworks.api.network.ITransferHandler;
import sonar.fluxnetworks.api.tiles.IFluxConnector;
import sonar.fluxnetworks.common.connection.FluxNetworkBase;
import sonar.fluxnetworks.common.connection.FluxNetworkServer;
import sonar.fluxnetworks.common.connection.NetworkStatistics;

import javax.annotation.Nonnull;
import java.util.List;

@Mixin(value = FluxNetworkServer.class,remap = false)
public abstract class MixinFluxNetworkServer extends FluxNetworkBase {

    @Shadow
    public long bufferLimiter;

    @Shadow
    @Nonnull
    public abstract <T extends IFluxConnector> List<T> getConnections(FluxLogicType type);

    @Unique
    private ITransferHandler novaEngineering_Core$instance;

    @WrapOperation(method = "onEndServerTick", at = @At(value = "INVOKE", target = "Lsonar/fluxnetworks/api/network/ITransferHandler;getRequest()J",ordinal = 1))
    public long getRequestMixin(ITransferHandler instance, Operation<Long> original) {
        this.novaEngineering_Core$instance = instance;
        return original.call(instance);
    }

    @Inject(method = "onEndServerTick", at = @At(value = "INVOKE", target = "Lsonar/fluxnetworks/api/network/ITransferHandler;getRequest()J",ordinal = 1,shift = At.Shift.AFTER), cancellable = true)
    public void getRequestI(CallbackInfo ci) {
        if (novaEngineering_Core$instance instanceof DreamEnergyPortHandler){
            this.bufferLimiter = Long.MAX_VALUE;
            ((NetworkStatistics)this.network_stats.getValue()).stopProfiling();
            ci.cancel();
        }
    }
}
