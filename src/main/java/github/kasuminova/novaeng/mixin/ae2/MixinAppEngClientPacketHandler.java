package github.kasuminova.novaeng.mixin.ae2;

import appeng.core.sync.AppEngPacket;
import appeng.core.sync.PacketCallState;
import appeng.core.sync.network.AppEngClientPacketHandler;
import github.kasuminova.novaeng.common.profiler.CPacketProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AppEngClientPacketHandler.class)
public class MixinAppEngClientPacketHandler {

    @SuppressWarnings("MethodMayBeStatic")
    @Redirect(
        method = "onPacketData",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/core/sync/AppEngPacket;setCallParam(Lappeng/core/sync/PacketCallState;)V",
            remap = false
        ),
        remap = false
    )
    private void redirectParsePacket(AppEngPacket instance, PacketCallState call) {
        CPacketProfiler.onPacketReceived(instance, instance.getPacketID());
        instance.setCallParam(call);
    }

}
