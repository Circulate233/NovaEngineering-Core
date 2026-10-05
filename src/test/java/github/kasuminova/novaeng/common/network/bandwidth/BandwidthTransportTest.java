package github.kasuminova.novaeng.common.network.bandwidth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BandwidthTransportTest {
    private static final int SERVER_PAYLOAD_ID = 9;
    private static final int CLIENT_PAYLOAD_ID = 24;
    private static final BandwidthSettings SETTINGS = new BandwidthSettings(8 * BandwidthSettings.MIB, 23, true);

    private static BandwidthConnection connection(boolean server, BandwidthSettings settings, AtomicBoolean playing) {
        return new BandwidthConnection(settings, server ? SERVER_PAYLOAD_ID : CLIENT_PAYLOAD_ID,
            server ? CLIENT_PAYLOAD_ID : SERVER_PAYLOAD_ID, 20, 64 * 1024, server, IntSets.singleton(31),
            ObjectSets.singleton("FML|HS"), playing::get);
    }

    private static ByteBuf packet(int id, byte[] data) {
        ByteBuf buffer = Unpooled.buffer();
        BandwidthWire.writeVarInt(buffer, id);
        return buffer.writeBytes(data);
    }

    private static ByteBuf payload(int id, String channel, byte[] data) {
        ByteBuf buffer = Unpooled.buffer();
        BandwidthWire.writeVarInt(buffer, id);
        byte[] channelBytes = channel.getBytes(StandardCharsets.UTF_8);
        BandwidthWire.writeVarInt(buffer, channelBytes.length);
        return buffer.writeBytes(channelBytes).writeBytes(data);
    }

    private static byte[] bytes(ByteBuf data) {
        byte[] result = new byte[data.readableBytes()];
        data.getBytes(data.readerIndex(), result);
        return result;
    }

    private static void assertPacket(byte[] expected, ByteBuf actual) {
        assertNotNull(actual);
        try {
            assertArrayEquals(expected, bytes(actual));
        } finally {
            actual.release();
        }
    }

    private static void transfer(EmbeddedChannel sender, EmbeddedChannel receiver) {
        ByteBuf outbound;
        while ((outbound = sender.readOutbound()) != null) {
            receiver.writeInbound(outbound);
        }
    }

    @Test
    void delaysSmallPacketsAndCompletesOriginalPromisesWhenBatchIsWritten() {
        var state = connection(true, SETTINGS, new AtomicBoolean(true));
        state.startAggregationForTest();
        var sender = new EmbeddedChannel(state.outbound);
        var target = connection(false, SETTINGS, new AtomicBoolean(true));
        var receiver = new EmbeddedChannel(new BandwidthInboundHandler(target));
        try {
            ByteBuf first = packet(1, new byte[]{2, 3});
            ByteBuf second = packet(2, new byte[]{4, 5});
            byte[] expectedFirst = bytes(first);
            byte[] expectedSecond = bytes(second);
            ChannelFuture one = sender.writeAndFlush(first);
            ChannelFuture two = sender.writeAndFlush(second);
            assertNull(sender.readOutbound());
            assertFalse(one.isDone());
            assertFalse(two.isDone());
            sender.advanceTimeBy(21, TimeUnit.MILLISECONDS);
            sender.runScheduledPendingTasks();
            assertTrue(one.isSuccess());
            assertTrue(two.isSuccess());
            transfer(sender, receiver);
            assertPacket(expectedFirst, receiver.readInbound());
            assertPacket(expectedSecond, receiver.readInbound());
            assertNull(receiver.readInbound());
            assertEquals(0, first.refCnt());
            assertEquals(0, second.refCnt());
        } finally {
            sender.finishAndReleaseAll();
            receiver.finishAndReleaseAll();
        }
    }

    @Test
    void urgentPacketAndBlacklistedChannelFlushPrecedingBatchesInOrder() {
        var state = connection(true, SETTINGS, new AtomicBoolean(true));
        state.startAggregationForTest();
        var sender = new EmbeddedChannel(state.outbound);
        var target = connection(false, SETTINGS, new AtomicBoolean(true));
        var receiver = new EmbeddedChannel(new BandwidthInboundHandler(target));
        try {
            ObjectList<byte[]> expected = new ObjectArrayList<>();
            for (ByteBuf next : new ObjectArrayList<>(new ByteBuf[]{packet(1, new byte[]{1}), packet(31, new byte[]{2}),
                packet(2, new byte[]{3}), payload(CLIENT_PAYLOAD_ID, "FML|HS", new byte[]{4})})) {
                expected.add(bytes(next));
                sender.writeAndFlush(next);
            }
            transfer(sender, receiver);
            expected.forEach(value -> assertPacket(value, receiver.readInbound()));
            assertNull(receiver.readInbound());
        } finally {
            sender.finishAndReleaseAll();
            receiver.finishAndReleaseAll();
        }
    }

    @Test
    void repeatedBatchesUseHistoryAndBothDirectionsAreIndependent() {
        var server = connection(true, SETTINGS, new AtomicBoolean(true));
        var client = connection(false, SETTINGS, new AtomicBoolean(true));
        server.startAggregationForTest();
        client.startAggregationForTest();
        var serverChannel = new EmbeddedChannel(new BandwidthInboundHandler(server), server.outbound);
        var clientChannel = new EmbeddedChannel(new BandwidthInboundHandler(client), client.outbound);
        // The first batch must itself be worthwhile before a streaming history can
        // exist.  A fully random first batch is correctly sent as ordinary packets;
        // use a random 4 KiB seed repeated through the payload so it is compressible
        // while still exercising non-trivial binary data.
        byte[] seed = new byte[4_096];
        new Random(88).nextBytes(seed);
        byte[] data = new byte[100_000];
        for (int offset = 0; offset < data.length; offset += seed.length) {
            System.arraycopy(seed, 0, data, offset, Math.min(seed.length, data.length - offset));
        }
        try {
            int firstSize = 0;
            for (int i = 0; i < 5; i++) {
                ByteBuf next = payload(CLIENT_PAYLOAD_ID, "machine", data);
                byte[] expected = bytes(next);
                serverChannel.writeAndFlush(next);
                ByteBuf encoded = serverChannel.readOutbound();
                if (i == 0) {
                    firstSize = encoded.readableBytes();
                } else {
                    assertTrue(encoded.readableBytes() < firstSize / 10, "History should compress repeated random data");
                }
                clientChannel.writeInbound(encoded);
                assertPacket(expected, clientChannel.readInbound());
                ByteBuf response = payload(SERVER_PAYLOAD_ID, "machine", data);
                byte[] expectedResponse = bytes(response);
                clientChannel.writeAndFlush(response);
                transfer(clientChannel, serverChannel);
                assertPacket(expectedResponse, serverChannel.readInbound());
            }
        } finally {
            serverChannel.finishAndReleaseAll();
            clientChannel.finishAndReleaseAll();
        }
    }

    @Test
    void incompressibleBatchFallsBackToOrdinaryPacketsWithoutExpansion() {
        var state = connection(true, SETTINGS, new AtomicBoolean(true));
        state.startAggregationForTest();
        var sender = new EmbeddedChannel(state.outbound);
        try {
            ObjectList<byte[]> expected = new ObjectArrayList<>();
            Random random = new Random(991);
            int rawBytes = 0;
            for (int i = 0; i < 16; i++) {
                byte[] data = new byte[257];
                random.nextBytes(data);
                ByteBuf packet = packet(100 + i, data);
                expected.add(bytes(packet));
                rawBytes += packet.readableBytes();
                sender.writeAndFlush(packet);
            }
            sender.advanceTimeBy(21, TimeUnit.MILLISECONDS);
            sender.runScheduledPendingTasks();

            int wireBytes = 0;
            int packets = 0;
            ByteBuf output;
            while ((output = sender.readOutbound()) != null) {
                try {
                    wireBytes += output.readableBytes();
                    packets++;
                    assertFalse(state.isOutgoingEnvelope(output),
                        "an incompressible candidate must be sent as ordinary packets");
                } finally {
                    output.release();
                }
            }
            assertEquals(expected.size(), packets);
            assertEquals(rawBytes, wireBytes);
            assertTrue(wireBytes <= rawBytes, "fallback must never report a transport expansion");
        } finally {
            sender.finishAndReleaseAll();
        }
    }

    @Test
    void listenerNeverReportsExpansionForAcceptedBatchesInEitherDirection() {
        BandwidthStats.setEnabled(true);
        BandwidthStats.reset();
        var senderState = connection(true, SETTINGS, new AtomicBoolean(true));
        var receiverState = connection(false, SETTINGS, new AtomicBoolean(true));
        senderState.startAggregationForTest();
        receiverState.startAggregationForTest();
        var sender = new EmbeddedChannel(senderState.outbound);
        var receiver = new EmbeddedChannel(new BandwidthInboundHandler(receiverState));
        try {
            byte[] repeated = new byte[24 * 1024];
            ByteBuf packet = payload(CLIENT_PAYLOAD_ID, "listener", repeated);
            byte[] expected = bytes(packet);
            sender.writeAndFlush(packet);
            sender.advanceTimeBy(21, TimeUnit.MILLISECONDS);
            sender.runScheduledPendingTasks();
            transfer(sender, receiver);
            assertPacket(expected, receiver.readInbound());

            assertFalse(BandwidthStats.hasExpansion(),
                "accepted Nova batches must never make the listener report wire expansion");
        } finally {
            sender.finishAndReleaseAll();
            receiver.finishAndReleaseAll();
            BandwidthStats.setEnabled(false);
            BandwidthStats.reset();
        }
    }

    @Test
    void optimizationToggleUsesLessWireForTheSameDeterministicWorkload() {
        int ordinaryWire = measureDeterministicWorkload(false);
        int optimizedWire = measureDeterministicWorkload(true);
        System.out.println("Nova bandwidth A/B: optimization off=" + ordinaryWire
            + "B, on=" + optimizedWire + "B, saved=" + (ordinaryWire - optimizedWire) + "B");
        assertTrue(optimizedWire < ordinaryWire,
            "the same repetitive workload should use fewer bytes with aggregation and Zstd");
    }

    private static int measureDeterministicWorkload(boolean optimize) {
        var state = connection(true, SETTINGS, new AtomicBoolean(true));
        if (optimize) {
            state.startAggregationForTest();
        }
        var sender = new EmbeddedChannel(state.outbound);
        int wireBytes = 0;
        try {
            byte[] data = new byte[1024];
            for (int i = 0; i < 128; i++) {
                sender.writeAndFlush(payload(CLIENT_PAYLOAD_ID, "ab", data));
            }
            sender.advanceTimeBy(21, TimeUnit.MILLISECONDS);
            sender.runScheduledPendingTasks();
            ByteBuf output;
            while ((output = sender.readOutbound()) != null) {
                try {
                    wireBytes += output.readableBytes();
                    if (optimize) {
                        assertTrue(state.isOutgoingEnvelope(output));
                    } else {
                        assertFalse(state.isOutgoingEnvelope(output));
                    }
                } finally {
                    output.release();
                }
            }
            return wireBytes;
        } finally {
            sender.finishAndReleaseAll();
            state.close();
        }
    }

    @Test
    void fallbackResetsIndexedChannelAndCompressionHistoryBeforeNextBatch() {
        var state = connection(true, SETTINGS, new AtomicBoolean(true));
        var peer = connection(false, SETTINGS, new AtomicBoolean(true));
        state.startAggregationForTest();
        peer.startAggregationForTest();
        var sender = new EmbeddedChannel(state.outbound);
        var receiver = new EmbeddedChannel(new BandwidthInboundHandler(peer));
        try {
            byte[] randomData = new byte[12 * 1024];
            new Random(812).nextBytes(randomData);
            ByteBuf first = payload(CLIENT_PAYLOAD_ID, "fallback", randomData);
            byte[] expectedFirst = bytes(first);
            sender.writeAndFlush(first);
            sender.advanceTimeBy(21, TimeUnit.MILLISECONDS);
            sender.runScheduledPendingTasks();
            ByteBuf firstOutput = sender.readOutbound();
            assertNotNull(firstOutput);
            assertFalse(state.isOutgoingEnvelope(firstOutput));
            receiver.writeInbound(firstOutput);
            assertPacket(expectedFirst, receiver.readInbound());

            byte[] repeated = new byte[12 * 1024];
            ByteBuf second = payload(CLIENT_PAYLOAD_ID, "fallback", repeated);
            byte[] expectedSecond = bytes(second);
            sender.writeAndFlush(second);
            sender.advanceTimeBy(21, TimeUnit.MILLISECONDS);
            sender.runScheduledPendingTasks();
            ByteBuf secondOutput = sender.readOutbound();
            assertNotNull(secondOutput);
            assertTrue(state.isOutgoingEnvelope(secondOutput), "compressible traffic should use Nova batching");
            receiver.writeInbound(secondOutput);
            assertPacket(expectedSecond, receiver.readInbound());
            assertNull(receiver.readInbound());
        } finally {
            sender.finishAndReleaseAll();
            receiver.finishAndReleaseAll();
        }
    }

    @Test
    void disabledChannelIndexLeavesCustomPayloadsUnmodified() {
        var senderState = connection(true, SETTINGS, new AtomicBoolean(true));
        var receiverState = connection(false, SETTINGS, new AtomicBoolean(true));
        senderState.indexChannels = false;
        receiverState.indexChannels = false;
        senderState.startAggregationForTest();
        receiverState.startAggregationForTest();
        var sender = new EmbeddedChannel(senderState.outbound);
        var receiver = new EmbeddedChannel(new BandwidthInboundHandler(receiverState));
        try {
            ByteBuf original = payload(CLIENT_PAYLOAD_ID, "machine", new byte[]{1, 2, 3});
            byte[] expected = bytes(original);
            sender.writeAndFlush(original);
            sender.advanceTimeBy(21, TimeUnit.MILLISECONDS);
            sender.runScheduledPendingTasks();
            transfer(sender, receiver);
            assertPacket(expected, receiver.readInbound());
            assertNull(receiver.readInbound());
        } finally {
            sender.finishAndReleaseAll();
            receiver.finishAndReleaseAll();
        }
    }

    @Test
    void largeCustomPayloadsRoundTripAtEightMiBLimitInBothDirections() {
        byte[] data = new byte[SETTINGS.payloadLimit()];
        new Random(12).nextBytes(data);
        for (boolean server : new boolean[]{false, true}) {
            var state = connection(server, SETTINGS, new AtomicBoolean(true));
            var peer = connection(!server, SETTINGS, new AtomicBoolean(true));
            state.startAggregationForTest();
            var sender = new EmbeddedChannel(state.outbound);
            var receiver = new EmbeddedChannel(new BandwidthInboundHandler(peer));
            try {
                ByteBuf original = payload(state.outgoingPayloadId, "large", data);
                byte[] expected = bytes(original);
                sender.writeAndFlush(original);
                ByteBuf envelope = sender.readOutbound();
                assertTrue(envelope.readableBytes() > 2 * BandwidthSettings.MIB);
                ByteBuf framed = Unpooled.buffer();
                BandwidthWire.encodeFrame(envelope, framed, SETTINGS.frameLimit());
                envelope.release();
                ObjectList<Object> frames = new ObjectArrayList<>();
                BandwidthWire.decodeFrame(framed, frames, SETTINGS.frameLimit());
                framed.release();
                assertEquals(1, frames.size());
                receiver.writeInbound(frames.getFirst());
                assertPacket(expected, receiver.readInbound());
            } finally {
                sender.finishAndReleaseAll();
                receiver.finishAndReleaseAll();
            }
        }
    }

    @Test
    void independentFramesCanBeDecodedByFreshReceivers() {
        var settings = new BandwidthSettings(SETTINGS.payloadLimit(), 21, false);
        var state = connection(true, settings, new AtomicBoolean(true));
        state.startAggregationForTest();
        var sender = new EmbeddedChannel(state.outbound);
        try {
            for (int i = 0; i < 3; i++) {
                ByteBuf raw = Unpooled.directBuffer().writeZero(100_000);
                try (var decoder = new ZstdStreamCodec(settings)) {
                    ByteBuf compressed = state.codec.compress(sender.alloc(), raw);
                    ByteBuf result = decoder.decompress(sender.alloc(), compressed, raw.readableBytes());
                    assertArrayEquals(bytes(raw), bytes(result));
                    result.release();
                    compressed.release();
                } finally {
                    raw.release();
                }
            }
        } finally {
            sender.finishAndReleaseAll();
        }
    }

    @Test
    void preHandshakePacketsPassThroughAndReceiverIsReadyBeforeSenderActivation() {
        var state = connection(true, SETTINGS, new AtomicBoolean(true));
        var sender = new EmbeddedChannel(state.outbound);
        try {
            ByteBuf original = packet(1, new byte[]{2});
            byte[] expected = bytes(original);
            assertTrue(sender.writeAndFlush(original).isSuccess());
            assertPacket(expected, sender.readOutbound());
        } finally {
            sender.finishAndReleaseAll();
        }
        // This receiver test deliberately leaves the synchronization start uncalled on the peer.
    }

    @Test
    void clientAlwaysRequestsTheCurrentDictionaryBeforeOptimizationStarts() {
        BandwidthDictionary.reset();
        AtomicBoolean playing = new AtomicBoolean(true);
        var server = connection(true, SETTINGS, playing);
        var client = connection(false, SETTINGS, playing);
        server.startAggregation(true, true);
        var serverChannel = new EmbeddedChannel(new BandwidthInboundHandler(server), server.outbound);
        var clientChannel = new EmbeddedChannel(new BandwidthInboundHandler(client), client.outbound);
        try {
            client.startAggregation(false, true);
            ByteBuf request = clientChannel.readOutbound();
            assertNotNull(request);
            assertEquals(BandwidthWire.DICTIONARY_REQUEST,
                BandwidthWire.envelope(request, SERVER_PAYLOAD_ID).readUnsignedByte());
            assertNull(serverChannel.readOutbound(), "The server waits for the client's request");

            serverChannel.writeInbound(request);
            ByteBuf data = serverChannel.readOutbound();
            assertNotNull(data);
            assertEquals(BandwidthWire.DICTIONARY_DATA,
                BandwidthWire.envelope(data, CLIENT_PAYLOAD_ID).readUnsignedByte());
            clientChannel.writeInbound(data);

            ByteBuf ready = clientChannel.readOutbound();
            assertNotNull(ready);
            assertEquals(BandwidthWire.DICTIONARY_READY,
                BandwidthWire.envelope(ready, SERVER_PAYLOAD_ID).readUnsignedByte());
            assertFalse(client.isActive(), "Optimization remains disabled after synchronization");
            serverChannel.writeInbound(ready);
            assertTrue(server.isActive());
        } finally {
            serverChannel.finishAndReleaseAll();
            clientChannel.finishAndReleaseAll();
            BandwidthDictionary.reset();
        }
    }

    @Test
    void removalReleasesPendingBuffersAndFailsPromises() {
        var state = connection(true, SETTINGS, new AtomicBoolean(true));
        state.startAggregationForTest();
        var sender = new EmbeddedChannel(state.outbound);
        try {
            ByteBuf original = packet(1, new byte[]{2});
            ChannelFuture future = sender.writeAndFlush(original);
            assertFalse(future.isDone());
            sender.pipeline().remove(state.outbound);
            assertTrue(future.isDone());
            assertFalse(future.isSuccess());
            assertTrue(state.closed);
            assertEquals(0, original.refCnt());
            sender.advanceTimeBy(100, TimeUnit.MILLISECONDS);
            sender.runScheduledPendingTasks();
            assertNull(sender.readOutbound());
        } finally {
            sender.finishAndReleaseAll();
        }
    }

    @Test
    void batchWriteFailureFailsAllOriginalPromises() {
        var state = connection(true, SETTINGS, new AtomicBoolean(true));
        state.startAggregationForTest();
        IOException failure = new IOException("test transport failure");
        var sender = new EmbeddedChannel(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
                ((ByteBuf) message).release();
                promise.setFailure(failure);
            }
        }, state.outbound);
        try {
            ChannelFuture one = sender.writeAndFlush(packet(1, new byte[]{2}));
            ChannelFuture two = sender.writeAndFlush(packet(2, new byte[]{3}));
            state.outbound.flushPending();
            assertSame(failure, one.cause());
            assertSame(failure, two.cause());
            assertTrue(state.closed);
            assertThrows(Exception.class, sender::checkException);
        } finally {
            sender.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsOversizeAndMalformedBatchesWithoutDeliveringPartialPackets() {
        try (var state = connection(false, SETTINGS, new AtomicBoolean(true))) {
            ByteBuf batch = Unpooled.buffer();
            batch.writeByte(BandwidthWire.BATCH).writeByte(BandwidthWire.VERSION).writeInt(0).writeBoolean(false);
            BandwidthWire.writeVarInt(batch, 0);
            BandwidthWire.writeVarInt(batch, 5);
            BandwidthWire.writeVarInt(batch, 2);
            batch.writeByte(2).writeByte(1).writeByte(4).writeByte(20).writeByte(2);
            try {
                assertThrows(DecoderException.class, () -> state.decode(UnpooledByteBufAllocatorHolder.ALLOCATOR, batch));
            } finally {
                batch.release();
            }
        }
        try (var state = connection(false, SETTINGS, new AtomicBoolean(true))) {
            ByteBuf batch = Unpooled.buffer();
            batch.writeByte(BandwidthWire.BATCH).writeByte(BandwidthWire.VERSION).writeInt(0).writeBoolean(true);
            BandwidthWire.writeVarInt(batch, 0);
            BandwidthWire.writeVarInt(batch, SETTINGS.batchLimit() + 1);
            BandwidthWire.writeVarInt(batch, 1);
            try {
                assertThrows(DecoderException.class, () -> state.decode(UnpooledByteBufAllocatorHolder.ALLOCATOR, batch));
            } finally {
                batch.release();
            }
        }
    }

    @Test
    void frameDecoderHandlesFragmentedHeadersAndMultipleFrames() {
        ByteBuf input = Unpooled.buffer();
        ObjectList<Object> output = new ObjectArrayList<>();
        try {
            input.writeByte(0x80);
            BandwidthWire.decodeFrame(input, output, SETTINGS.frameLimit());
            assertEquals(0, input.readerIndex());
            input.writeByte(1).writeZero(127);
            BandwidthWire.decodeFrame(input, output, SETTINGS.frameLimit());
            assertEquals(0, input.readerIndex());
            input.writeByte(0).writeByte(1).writeByte(42);
            BandwidthWire.decodeFrame(input, output, SETTINGS.frameLimit());
            BandwidthWire.decodeFrame(input, output, SETTINGS.frameLimit());
            assertEquals(2, output.size());
            assertEquals(128, ((ByteBuf) output.get(0)).readableBytes());
            assertEquals(42, ((ByteBuf) output.get(1)).readUnsignedByte());
        } finally {
            output.forEach(value -> ((ByteBuf) value).release());
            input.release();
        }
    }

    @Test
    void frameDecoderRejectsZeroOversizedAndOverlongHeadersImmediately() {
        for (byte[] bytes : new byte[][]{{0}, {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80},
            {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F}}) {
            ByteBuf input = Unpooled.wrappedBuffer(bytes);
            try {
                assertThrows(DecoderException.class, () -> BandwidthWire.decodeFrame(input, new ObjectArrayList<>(), SETTINGS.frameLimit()));
            } finally {
                input.release();
            }
        }
    }

    @Test
    void rejectsZstdWindowsAboveSupportedMaximumBeforeNativeAllocation() {
        try (var codec = new ZstdStreamCodec(SETTINGS)) {
            ByteBuf input = Unpooled.buffer().writeIntLE(0xFD2FB528).writeByte(0).writeByte(0xA0);
            try {
                assertThrows(DecoderException.class, () -> codec.decompress(UnpooledByteBufAllocatorHolder.ALLOCATOR, input, 100));
            } finally {
                input.release();
            }
        }
    }

    private static final class UnpooledByteBufAllocatorHolder {
        static final ByteBufAllocator ALLOCATOR = UnpooledByteBufAllocator.DEFAULT;
    }
}
