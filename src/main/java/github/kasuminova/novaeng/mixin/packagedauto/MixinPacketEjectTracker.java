package github.kasuminova.novaeng.mixin.packagedauto;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thelm.packagedauto.network.packet.PacketEjectTracker;

@Mixin(value = PacketEjectTracker.class, remap = false)
public class MixinPacketEjectTracker {

    @Inject(method = "onMessage", at = @At("HEAD"), cancellable = true)
    public void onMessage(MessageContext ctx, CallbackInfoReturnable<IMessage> cir) {
        cir.setReturnValue(null);
    }
}
