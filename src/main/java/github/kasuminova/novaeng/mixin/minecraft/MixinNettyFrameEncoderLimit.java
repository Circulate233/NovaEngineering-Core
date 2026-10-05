package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.network.bandwidth.BandwidthWire;
import github.kasuminova.novaeng.common.network.bandwidth.NetworkLimits;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.local.LocalChannel;
import net.minecraft.network.NettyVarint21FrameEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NettyVarint21FrameEncoder.class)
public class MixinNettyFrameEncoderLimit {
    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Lio/netty/buffer/ByteBuf;)V", at = @At("HEAD"), cancellable = true)
    private void novaeng$boundedFourByteFrames(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, ByteBuf byteBuf2, CallbackInfo ci) {
        // LocalChannel passes Packet objects directly and normally never reaches this
        // handler. Keep the mixin transparent if another mod adds a ByteBuf stage to it.
        if (channelHandlerContext.channel() instanceof LocalChannel) {
            return;
        }
        BandwidthWire.encodeFrame(byteBuf, byteBuf2, NetworkLimits.frame());
        ci.cancel();
    }
}
