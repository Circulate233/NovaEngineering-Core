package github.kasuminova.novaeng.common.network.bandwidth;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.ScheduledFuture;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BandwidthOutboundHandler extends ChannelOutboundHandlerAdapter {
    private final BandwidthConnection connection;
    private ObjectList<ChannelPromise> promises = new ObjectArrayList<>();
    private ObjectList<ByteBuf> pendingPackets = new ObjectArrayList<>();
    private ChannelHandlerContext context;
    private ByteBuf pending;
    private int pendingOriginalBytes;
    private int pendingDirectBytes;
    private int pendingIndexedDelta;
    private ScheduledFuture<?> scheduled;

    public BandwidthOutboundHandler(BandwidthConnection connection) {
        this.connection = connection;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        context = ctx;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
        if (!(message instanceof ByteBuf packet)) {
            flushPending();
            ctx.write(message, promise);
            return;
        }
        try {
            if (connection.closed) {
                throw new ClosedChannelException();
            }
            connection.observeOutgoing(packet);
            connection.refreshDictionary();
            if (connection.immediate(packet)) {
                flushPending();
                if (connection.closed) {
                    throw new ClosedChannelException();
                }
                ctx.write(packet.retain(), promise);
                BandwidthStats.outgoingPacket(packet.readableBytes());
                return;
            }
            BandwidthWire.validatePacket(packet, connection.outgoingPayloadId, connection.settings.payloadLimit(), connection.settings.packetLimit());
            int size = packet.readableBytes();
            int originalEntrySize = size + BandwidthWire.varIntSize(size);
            int maximumEntrySize = size + 8 + BandwidthWire.varIntSize(size + 8);
            if (pending != null && (pending.readableBytes() + maximumEntrySize > connection.targetBytes
                || pendingOriginalBytes + originalEntrySize > connection.settings.batchLimit())) {
                flushPending();
                if (connection.closed) {
                    throw new ClosedChannelException();
                }
            }
            if (pending == null) {
                pending = ctx.alloc().directBuffer(Math.min(connection.targetBytes, connection.settings.batchLimit()), connection.settings.batchLimit());
                if (!connection.settings.reuseContext()) {
                    connection.outgoingChannels.clear();
                }
            }
            pendingPackets.add(packet.retain());
            ByteBuf indexed = connection.outgoingChannels.encode(ctx.alloc(), packet, connection.outgoingPayloadId, connection.indexChannels);
            try {
                BandwidthWire.writeVarInt(pending, indexed.readableBytes());
                pending.writeBytes(indexed, indexed.readerIndex(), indexed.readableBytes());
                int indexedDelta = size - indexed.readableBytes();
                pendingIndexedDelta += indexedDelta;
            } finally {
                indexed.release();
            }
            pendingOriginalBytes += originalEntrySize;
            pendingDirectBytes += size;
            promises.add(promise);
            if (pending.readableBytes() >= connection.targetBytes || promises.size() >= BandwidthSettings.MAX_PACKETS || !ctx.channel().isWritable()) {
                flushPending();
            } else if (scheduled == null) {
                scheduled = ctx.executor().schedule(this::flushPending, connection.delayMillis, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable failure) {
            promise.tryFailure(failure);
            fail(failure);
        } finally {
            packet.release();
        }
    }

    @Override
    public void flush(ChannelHandlerContext ctx) {
        // Vanilla calls writeAndFlush per packet. The connection timer bounds batching latency.
        ctx.flush();
    }

    public void flushPending() {
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
        if (pending == null) {
            return;
        }
        ByteBuf raw = pending;
        pending = null;
        pendingOriginalBytes = 0;
        int directBytes = pendingDirectBytes;
        pendingDirectBytes = 0;
        int indexedDelta = pendingIndexedDelta;
        pendingIndexedDelta = 0;
        ObjectList<ByteBuf> batchPackets = pendingPackets;
        pendingPackets = new ObjectArrayList<>();
        ObjectList<ChannelPromise> batchPromises = promises;
        promises = new ObjectArrayList<>();
        ByteBuf encoded = null;
        boolean transferred = false;
        try {
            encoded = connection.encode(context.alloc(), raw, batchPromises.size());
            // A Nova envelope is an optimization candidate.  Its complete encoded
            // size, including the custom-payload header and batch metadata, must be
            // strictly smaller than the ordinary packets it replaces.
            if (encoded.readableBytes() >= directBytes) {
                encoded.release();
                encoded = null;
                connection.discardUnsentBatch();
                Throwable[] fallbackFailure = new Throwable[1];
                AtomicBoolean fallbackReturned = new AtomicBoolean();
                AtomicBoolean fallbackReported = new AtomicBoolean();
                for (int i = 0; i < batchPackets.size(); i++) {
                    ByteBuf packet = batchPackets.get(i);
                    int packetBytes = packet.readableBytes();
                    ChannelPromise originalPromise = batchPromises.get(i);
                    originalPromise.addListener(future -> {
                        if (!future.isSuccess()) {
                            fallbackFailure[0] = future.cause();
                            if (fallbackReturned.get() && fallbackReported.compareAndSet(false, true)) {
                                fail(future.cause());
                            }
                        }
                    });
                    context.write(packet, originalPromise);
                    batchPackets.set(i, null);
                    BandwidthStats.outgoingPacket(packetBytes);
                }
                context.flush();
                transferred = true;
                fallbackReturned.set(true);
                if (fallbackFailure[0] != null && fallbackReported.compareAndSet(false, true)) {
                    fail(fallbackFailure[0]);
                }
                return;
            }
            connection.headerBytesSaved += indexedDelta;
            BandwidthStats.indexedDelta(indexedDelta);
            connection.recordOutgoingBatch(directBytes, encoded.readableBytes(), batchPromises.size());
            ChannelPromise completion = context.newPromise();
            completion.addListener(future -> {
                for (ChannelPromise original : batchPromises) {
                    if (future.isSuccess()) {
                        original.trySuccess();
                    } else {
                        original.tryFailure(future.cause());
                    }
                }
                if (!future.isSuccess()) {
                    fail(future.cause());
                }
            });
            context.writeAndFlush(encoded, completion);
            transferred = true;
            batchPackets.forEach(ByteBuf::release);
        } catch (Throwable failure) {
            if (!transferred && encoded != null) {
                encoded.release();
            }
            batchPackets.forEach(packet -> {
                if (packet != null) {
                    packet.release();
                }
            });
            batchPromises.forEach(promise -> promise.tryFailure(failure));
            fail(failure);
        } finally {
            raw.release();
        }
    }

    void sendDictionaryRequest(int version) {
        ByteBuf packet = context.alloc().buffer(32);
        BandwidthWire.dictionaryRequest(packet, connection.outgoingPayloadId, version);
        sendControl(packet);
    }

    void sendDictionaryData(int version, byte[] dictionary) {
        ByteBuf packet = context.alloc().buffer(dictionary.length + 32);
        BandwidthWire.dictionaryData(packet, connection.outgoingPayloadId, version, dictionary);
        sendControl(packet);
    }

    void sendDictionaryReady(int version) {
        ByteBuf packet = context.alloc().buffer(32);
        BandwidthWire.dictionaryReady(packet, connection.outgoingPayloadId, version);
        sendControl(packet);
    }

    private void sendControl(ByteBuf packet) {
        try {
            flushPending();
            // Dictionary synchronization is ordinary CustomPayload traffic too. Count it
            // so the disconnect summary covers the complete Nova transport interval.
            BandwidthStats.outgoingPacket(packet.readableBytes());
            context.writeAndFlush(packet);
        } catch (Throwable failure) {
            packet.release();
            fail(failure);
        }
    }

    private void fail(Throwable failure) {
        connection.close();
        context.fireExceptionCaught(failure);
        context.close();
    }

    void discard() {
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
        if (pending != null) {
            pending.release();
            pending = null;
        }
        pendingOriginalBytes = 0;
        pendingDirectBytes = 0;
        pendingIndexedDelta = 0;
        pendingPackets.forEach(ByteBuf::release);
        pendingPackets.clear();
        ClosedChannelException failure = new ClosedChannelException();
        promises.forEach(promise -> promise.tryFailure(failure));
        promises.clear();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
        flushPending();
        ctx.close(promise);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        connection.close();
    }
}
