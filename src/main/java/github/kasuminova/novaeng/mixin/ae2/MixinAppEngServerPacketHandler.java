package github.kasuminova.novaeng.mixin.ae2;

import appeng.core.sync.AppEngPacket;
import appeng.core.sync.PacketCallState;
import appeng.core.sync.network.AppEngServerPacketHandler;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.common.profiler.SPacketProfiler;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AppEngServerPacketHandler.class)
public class MixinAppEngServerPacketHandler {

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
    private void redirectParsePacket(AppEngPacket instance, PacketCallState call,
                                     @Local(name = "player") EntityPlayer player) {
        if (player != null) {
            SPacketProfiler.onPacketReceived(player, instance);
        }
        instance.setCallParam(call);
    }

}
