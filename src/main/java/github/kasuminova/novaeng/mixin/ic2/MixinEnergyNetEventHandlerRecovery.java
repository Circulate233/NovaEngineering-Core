package github.kasuminova.novaeng.mixin.ic2;

import github.kasuminova.novaeng.common.integration.ic2.energy.IC2EnergyNetRecovery;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.core.energy.grid.EventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Notifications refresh third-party caches without queuing duplicate IC2 additions/removals.
 */
@Mixin(value = EventHandler.class, remap = false)
public abstract class MixinEnergyNetEventHandlerRecovery {
    @Inject(method = "onEnergyTileLoad", at = @At("HEAD"), cancellable = true, require = 1)
    private void nova$skipLoadNotification(EnergyTileLoadEvent event, CallbackInfo ci) {
        if (IC2EnergyNetRecovery.isNotificationEvent(event)) ci.cancel();
    }

    @Inject(method = "onEnergyTileUnload", at = @At("HEAD"), cancellable = true, require = 1)
    private void nova$skipUnloadNotification(EnergyTileUnloadEvent event, CallbackInfo ci) {
        if (IC2EnergyNetRecovery.isNotificationEvent(event)) ci.cancel();
    }
}
