package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.network.bandwidth.BandwidthWire;
import github.kasuminova.novaeng.common.network.bandwidth.NetworkLimits;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.local.LocalChannel;
import net.minecraft.network.NettyVarint21FrameDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(NettyVarint21FrameDecoder.class)
public class MixinNettyFrameDecoderLimit {
    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void novaeng$boundedFourByteFrames(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list, CallbackInfo ci) {
        // LocalChannel passes Packet objects directly and normally never reaches this
        // handler. Keep the mixin transparent if another mod adds a ByteBuf stage to it.
        if (channelHandlerContext.channel() instanceof LocalChannel) {
            return;
        }
        BandwidthWire.decodeFrame(byteBuf, list, NetworkLimits.frame());
        ci.cancel();
    }
}
