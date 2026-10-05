package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.network.bandwidth.NovaBandwidth;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NettyCompressionEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NettyCompressionEncoder.class)
public class MixinNettyCompressionEncoderBandwidth {
    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Lio/netty/buffer/ByteBuf;)V", at = @At("HEAD"), cancellable = true)
    private void novaeng$avoidDoubleCompression(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, ByteBuf byteBuf2, CallbackInfo ci) {
        if (NovaBandwidth.bypassZlib(channelHandlerContext, byteBuf)) {
            // Vanilla's zero data-length marker explicitly represents an uncompressed frame.
            byteBuf2.writeByte(0);
            byteBuf2.writeBytes(byteBuf, byteBuf.readerIndex(), byteBuf.readableBytes());
            ci.cancel();
        }
    }
}
