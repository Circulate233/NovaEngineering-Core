package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.network.bandwidth.NovaBandwidth;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NettyCompressionEncoder;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.zip.Deflater;

@Mixin(NettyCompressionEncoder.class)
public abstract class MixinNettyCompressionEncoderBandwidth {
    @Unique
    private static final int NOVA_MAX_CACHED_INPUT = 65536;

    @Shadow
    private int threshold;
    @Final
    @Shadow
    private byte[] buffer;
    @Final
    @Shadow
    private Deflater deflater;

    @Unique
    private byte[] nova$compressionInput = new byte[0];

    /**
     * @author circulation
     * @reason Keep Nova's aggregated-packet bypass and reuse vanilla's transient Zlib input
     * buffer. Buffers above the cache cap remain one-shot allocations.
     */
    @Overwrite
    protected void encode(final ChannelHandlerContext context, final ByteBuf input, final ByteBuf output) {
        if (NovaBandwidth.bypassZlib(context, input)) {
            output.writeByte(0);
            output.writeBytes(input, input.readerIndex(), input.readableBytes());
            return;
        }

        if (!NovaEngCoreConfig.NETWORK.optimizeCompressionBuffers) {
            nova$encodeVanilla(input, output);
            return;
        }

        final PacketBuffer packetBuffer = new PacketBuffer(output);
        final int readable = input.readableBytes();
        if (readable < this.threshold) {
            packetBuffer.writeVarInt(0);
            packetBuffer.writeBytes(input);
            return;
        }

        byte[] scratch = this.nova$compressionInput;
        if (scratch.length < readable) {
            scratch = new byte[readable];
            if (readable <= NOVA_MAX_CACHED_INPUT) {
                this.nova$compressionInput = scratch;
            }
        }
        input.readBytes(scratch, 0, readable);

        packetBuffer.writeVarInt(readable);
        this.deflater.setInput(scratch, 0, readable);
        this.deflater.finish();
        while (!this.deflater.finished()) {
            final int written = this.deflater.deflate(this.buffer);
            packetBuffer.writeBytes(this.buffer, 0, written);
        }
        this.deflater.reset();
    }

    @Unique
    private void nova$encodeVanilla(final ByteBuf input, final ByteBuf output) {
        final PacketBuffer packetBuffer = new PacketBuffer(output);
        final int readable = input.readableBytes();
        if (readable < this.threshold) {
            packetBuffer.writeVarInt(0);
            packetBuffer.writeBytes(input);
            return;
        }

        final byte[] source = new byte[readable];
        input.readBytes(source);
        packetBuffer.writeVarInt(readable);
        this.deflater.setInput(source);
        this.deflater.finish();
        while (!this.deflater.finished()) {
            final int written = this.deflater.deflate(this.buffer);
            packetBuffer.writeBytes(this.buffer, 0, written);
        }
        this.deflater.reset();
    }
}
