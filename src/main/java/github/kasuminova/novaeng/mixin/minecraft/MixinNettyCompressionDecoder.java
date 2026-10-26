package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.network.bandwidth.NetworkLimits;
import github.kasuminova.novaeng.common.profiler.CPacketProfiler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.NettyCompressionDecoder;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.zip.Inflater;

@Mixin(NettyCompressionDecoder.class)
public abstract class MixinNettyCompressionDecoder extends ByteToMessageDecoder {
    @Unique
    private static final int NOVA_MAX_CACHED_INPUT = 65536;

    @Shadow
    private int threshold;
    @Final
    @Shadow
    private Inflater inflater;

    @Unique
    private byte[] nova$compressionInput = new byte[0];

    /**
     * @author circulation
     * @reason Reuse only the compressed input; the inflated output escapes into Netty and stays
     * fresh. Preserve packet profiling and Nova's configurable decompression limit.
     */
    @Overwrite
    protected void decode(final ChannelHandlerContext context, final ByteBuf input,
                          final List<Object> output) throws Exception {
        if (input.readableBytes() == 0) {
            return;
        }

        final PacketBuffer packetBuffer = new PacketBuffer(input);
        final int decompressedLength = packetBuffer.readVarInt();
        if (decompressedLength == 0) {
            final int readable = packetBuffer.readableBytes();
            final ByteBuf decoded = packetBuffer.readBytes(readable);
            CPacketProfiler.onPacketDecoded(readable);
            output.add(decoded);
            return;
        }

        if (decompressedLength < this.threshold) {
            throw new DecoderException("Badly compressed packet - size of " + decompressedLength
                + " is below server threshold of " + this.threshold);
        }
        final int maximum = NetworkLimits.decompressed();
        if (decompressedLength > maximum) {
            throw new DecoderException("Badly compressed packet - size of " + decompressedLength
                + " is larger than protocol maximum of " + maximum);
        }

        final int compressedLength = packetBuffer.readableBytes();
        byte[] scratch = this.nova$compressionInput;
        if (NovaEngCoreConfig.NETWORK.optimizeCompressionBuffers) {
            if (scratch.length < compressedLength) {
                scratch = new byte[compressedLength];
                if (compressedLength <= NOVA_MAX_CACHED_INPUT) {
                    this.nova$compressionInput = scratch;
                }
            }
        } else {
            scratch = new byte[compressedLength];
        }
        packetBuffer.readBytes(scratch, 0, compressedLength);
        CPacketProfiler.onPacketDecoded(compressedLength);

        this.inflater.setInput(scratch, 0, compressedLength);
        final byte[] inflated = new byte[decompressedLength];
        try {
            this.inflater.inflate(inflated);
        } finally {
            this.inflater.reset();
        }
        output.add(Unpooled.wrappedBuffer(inflated));
    }
}
