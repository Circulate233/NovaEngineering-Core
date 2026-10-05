package github.kasuminova.novaeng.common.network.bandwidth;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ReferenceCountUtil;
import it.unimi.dsi.fastutil.objects.ObjectList;

public final class BandwidthInboundHandler extends ChannelInboundHandlerAdapter {
    private final BandwidthConnection connection;

    public BandwidthInboundHandler(BandwidthConnection connection) {
        this.connection = connection;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        if (!(message instanceof ByteBuf packet)) {
            ctx.fireChannelRead(message);
            return;
        }
        if (!connection.playing.getAsBoolean()) {
            ByteBuf control = BandwidthWire.envelope(packet, connection.incomingPayloadId);
            if (control == null || !control.isReadable()) {
                ctx.fireChannelRead(packet);
                return;
            }
            if (control.getUnsignedByte(control.readerIndex()) == BandwidthWire.BATCH) {
                packet.release();
                connection.close();
                ctx.fireExceptionCaught(new DecoderException("Nova bandwidth batch arrived before PLAY"));
                ctx.close();
                return;
            }
            try {
                connection.handleControl(control);
                BandwidthStats.incomingPacket(packet.readableBytes());
            } catch (Throwable failure) {
                connection.close();
                ctx.fireExceptionCaught(failure);
                ctx.close();
            } finally {
                packet.release();
            }
            return;
        }
        try {
            ByteBuf body = BandwidthWire.envelope(packet, connection.incomingPayloadId);
            if (body == null) {
                BandwidthStats.incomingPacket(packet.readableBytes());
                connection.observeIncoming(packet);
                ctx.fireChannelRead(packet.retain());
            } else if (connection.handleControl(body)) {
                BandwidthStats.incomingPacket(packet.readableBytes());
            } else {
                int wireBytes = packet.readableBytes();
                ObjectList<ByteBuf> subpackets;
                try {
                    subpackets = connection.decode(ctx.alloc(), body);
                } catch (BandwidthConnection.DictionaryMismatchException mismatch) {
                    connection.handleDictionaryMismatch(mismatch.version());
                    return;
                }
                int rawBytes = 0;
                for (ByteBuf subpacket : subpackets) {
                    rawBytes += subpacket.readableBytes();
                }
                BandwidthStats.incomingBatch(rawBytes, wireBytes, subpackets.size());
                int delivered = 0;
                try {
                    for (; delivered < subpackets.size(); delivered++) {
                        ctx.fireChannelRead(subpackets.get(delivered));
                    }
                } catch (Throwable failure) {
                    for (int i = delivered; i < subpackets.size(); i++) {
                        ReferenceCountUtil.release(subpackets.get(i));
                    }
                    throw failure;
                }
            }
        } catch (Throwable failure) {
            connection.close();
            ctx.fireExceptionCaught(failure);
            ctx.close();
        } finally {
            packet.release();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connection.close();
        super.channelInactive(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        connection.close();
    }
}
