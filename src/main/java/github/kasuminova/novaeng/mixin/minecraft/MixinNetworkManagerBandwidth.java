package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.network.bandwidth.NovaBandwidth;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManagerBandwidth {
    @Inject(method = "channelActive", at = @At("TAIL"))
    private void novaeng$installBandwidth(ChannelHandlerContext p_channelActive_1_, CallbackInfo ci) {
        NovaBandwidth.install((NetworkManager) (Object) this);
    }

    @Inject(method = "setConnectionState", at = @At("TAIL"))
    private void novaeng$installBandwidthAfterStateChange(EnumConnectionState state, CallbackInfo ci) {
        NovaBandwidth.install((NetworkManager) (Object) this);
    }

    @ModifyArg(method = "setCompressionThreshold", at = @At(value = "INVOKE",
        target = "Lio/netty/channel/ChannelPipeline;addBefore(Ljava/lang/String;Ljava/lang/String;Lio/netty/channel/ChannelHandler;)Lio/netty/channel/ChannelPipeline;"), index = 0)
    private String novaeng$compressionOutsideAggregation(String original) {
        return NovaBandwidth.compressionAnchor((NetworkManager) (Object) this, original);
    }
}
