package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.profiler.CPacketProfiler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.NettyCompressionDecoder;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("MethodMayBeStatic")
@Mixin(NettyCompressionDecoder.class)
public class MixinNettyCompressionDecoder {

    @Redirect(method = "decode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/PacketBuffer;readBytes(I)Lio/netty/buffer/ByteBuf;")
    )
    private ByteBuf onDecodeLen(final PacketBuffer packetBuffer, final int p_readBytes_1_) {
        final int prevIndex = packetBuffer.readerIndex();

        ByteBuf decoded = packetBuffer.readBytes(p_readBytes_1_);

        CPacketProfiler.onPacketDecoded(packetBuffer.readerIndex() - prevIndex);
        return decoded;
    }

    @Redirect(method = "decode",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/PacketBuffer;readBytes([B)Lio/netty/buffer/ByteBuf;")
    )
    private ByteBuf onDecodeByteArray(final PacketBuffer packetBuffer, final byte[] p_readBytes_1_) {
        final int prevIndex = packetBuffer.readerIndex();

        ByteBuf decoded = packetBuffer.readBytes(p_readBytes_1_);

        CPacketProfiler.onPacketDecoded(packetBuffer.readerIndex() - prevIndex);
        return decoded;
    }
}
