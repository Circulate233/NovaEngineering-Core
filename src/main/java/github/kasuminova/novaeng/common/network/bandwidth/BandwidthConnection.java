package github.kasuminova.novaeng.common.network.bandwidth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

import java.util.function.BooleanSupplier;

/** Protocol and native state for exactly one connection. No global queues or compression history. */
public final class BandwidthConnection implements AutoCloseable {
    private static final byte[] EMPTY_DICTIONARY = new byte[0];
    final BandwidthSettings settings;
    final int incomingPayloadId;
    final int outgoingPayloadId;
    final int delayMillis;
    final int targetBytes;
    final boolean server;
    final IntSet immediatePackets;
    final ObjectSet<String> immediateChannels;
    final BooleanSupplier playing;
    final ZstdStreamCodec codec;
    final BandwidthOutboundHandler outbound;
    final ChannelIndex outgoingChannels = new ChannelIndex();
    private final ChannelIndex incomingChannels = new ChannelIndex();
    boolean indexChannels = true;
    long headerBytesSaved;
    boolean active;
    boolean closed;
    private int sendSequence;
    private int receiveSequence;
    private long sentRawBytes;
    private long sentEnvelopeBytes;
    private long sentPackets;
    private int dictionaryVersion;
    private boolean dictionaryEnabled;
    private boolean optimizationEnabled;
    private boolean dictionaryRequestSent;
    private boolean resetCompressionPending;
    private boolean resetOutgoingChannels;
    private BandwidthDictionary.Snapshot pendingDictionary;

    public BandwidthConnection(BandwidthSettings settings, int incomingPayloadId, int outgoingPayloadId,
                               int delayMillis, int targetBytes, IntSet immediatePackets,
                               ObjectSet<String> immediateChannels, BooleanSupplier playing) {
        this(settings, incomingPayloadId, outgoingPayloadId, delayMillis, targetBytes, false,
            immediatePackets, immediateChannels, playing);
    }

    public BandwidthConnection(BandwidthSettings settings, int incomingPayloadId, int outgoingPayloadId,
                               int delayMillis, int targetBytes, boolean server, IntSet immediatePackets,
                               ObjectSet<String> immediateChannels, BooleanSupplier playing) {
        this.settings = settings;
        this.incomingPayloadId = incomingPayloadId;
        this.outgoingPayloadId = outgoingPayloadId;
        this.delayMillis = Math.clamp(delayMillis, 1, 20);
        this.targetBytes = Math.clamp(targetBytes, 4096, settings.batchLimit());
        this.server = server;
        this.immediatePackets = IntSets.unmodifiable(immediatePackets);
        this.immediateChannels = ObjectSets.unmodifiable(immediateChannels);
        this.playing = playing;
        this.codec = new ZstdStreamCodec(settings);
        this.outbound = new BandwidthOutboundHandler(this);
        if (server) {
            installDictionary(BandwidthDictionary.snapshot());
        }
    }

    public void startAggregation() {
        startAggregation(true, true);
    }

    public void startAggregation(boolean useDictionary) {
        startAggregation(true, useDictionary);
    }

    public void startAggregation(boolean optimize, boolean useDictionary) {
        if (!closed) {
            optimizationEnabled = optimize;
            dictionaryEnabled = useDictionary;
            // The client must refresh its dictionary on every completed connection. The
            // server answers with an empty version-zero dictionary when planning is disabled.
            active = false;
            if (!server) {
                dictionaryRequestSent = true;
                outbound.sendDictionaryRequest(dictionaryVersion);
            } else {
                // Wait for the client's request so initialization has one deterministic direction.
            }
        }
    }

    /** Test-only direct mode; production connections always perform dictionary synchronization. */
    void startAggregationForTest() {
        if (!closed) {
            optimizationEnabled = true;
            dictionaryEnabled = false;
            active = true;
        }
    }

    public boolean isActive() {
        return active && !closed;
    }

    boolean immediate(ByteBuf packet) {
        if (!isActive() || !playing.getAsBoolean()) {
            return true;
        }
        if (immediatePackets.contains(BandwidthWire.readVarInt(packet.duplicate()))) {
            return true;
        }
        String channel = BandwidthWire.customChannel(packet, outgoingPayloadId);
        return channel != null && (channel.equals(BandwidthWire.CHANNEL) || immediateChannels.contains(channel));
    }

    public boolean isOutgoingEnvelope(ByteBuf packet) {
        if (!playing.getAsBoolean()) {
            return false;
        }
        ByteBuf body = BandwidthWire.envelope(packet, outgoingPayloadId);
        return body != null && body.isReadable() && body.getUnsignedByte(body.readerIndex()) == BandwidthWire.BATCH;
    }

    ByteBuf encode(ByteBufAllocator allocator, ByteBuf raw, int count) {
        ByteBuf result = allocator.buffer();
        ByteBuf compressed = null;
        try {
            boolean resetCompression = resetCompressionPending;
            if (resetCompression) {
                codec.resetCompressor();
            }
            boolean compress = raw.readableBytes() >= 32;
            BandwidthWire.header(result, outgoingPayloadId, BandwidthWire.BATCH);
            result.writeByte(BandwidthWire.VERSION);
            result.writeInt(sendSequence);
            result.writeByte((compress ? 1 : 0)
                | ((!settings.reuseContext() || resetOutgoingChannels) ? 2 : 0)
                | (resetCompression ? 4 : 0));
            BandwidthWire.writeVarInt(result, dictionaryVersion);
            BandwidthWire.writeVarInt(result, raw.readableBytes());
            BandwidthWire.writeVarInt(result, count);
            if (compress) {
                compressed = codec.compress(allocator, raw);
                result.writeBytes(compressed);
            } else {
                result.writeBytes(raw, raw.readerIndex(), raw.readableBytes());
            }
            if (result.readableBytes() > settings.frameLimit()) {
                throw new IllegalStateException("Compressed envelope exceeds frame limit");
            }
            return result;
        } catch (Throwable failure) {
            result.release();
            throw failure;
        } finally {
            if (compressed != null) {
                compressed.release();
            }
        }
    }

    /** Validate the whole batch before delivering any subpacket to Minecraft/Forge. */
    ObjectList<ByteBuf> decode(ByteBufAllocator allocator, ByteBuf body) {
        if (closed || !playing.getAsBoolean()) {
            throw new DecoderException("Bandwidth envelope outside a live PLAY connection");
        }
        if (body.readUnsignedByte() != BandwidthWire.BATCH || body.readUnsignedByte() != BandwidthWire.VERSION) {
            throw new DecoderException("Unsupported Nova bandwidth protocol; both endpoints must use the same pack");
        }
        if (body.readInt() != receiveSequence++) {
            throw new DecoderException("Out-of-order Nova bandwidth batch");
        }
        int flags = body.readUnsignedByte();
        if (flags > 7) {
            throw new DecoderException("Unknown bandwidth flags");
        }
        int incomingDictionaryVersion = BandwidthWire.readVarInt(body);
        if (incomingDictionaryVersion < 0) {
            throw new DecoderException("Invalid bandwidth dictionary version");
        }
        if (incomingDictionaryVersion != dictionaryVersion) {
            throw new DictionaryMismatchException(incomingDictionaryVersion);
        }
        int rawSize = BandwidthWire.readVarInt(body);
        int count = BandwidthWire.readVarInt(body);
        if (rawSize <= 0 || rawSize > settings.batchLimit() || count <= 0 || count > BandwidthSettings.MAX_PACKETS
            || body.readableBytes() > settings.frameLimit()) {
            throw new DecoderException("Invalid bandwidth batch size or packet count");
        }
        ByteBuf raw;
        if ((flags & 4) != 0) {
            codec.resetDecompressor();
        }
        if ((flags & 2) != 0) {
            incomingChannels.clear();
        }
        if ((flags & 1) != 0) {
            raw = codec.decompress(allocator, body, rawSize);
        } else {
            if (body.readableBytes() != rawSize) {
                throw new DecoderException("Uncompressed batch length mismatch");
            }
            raw = body.retainedSlice();
        }
        ObjectList<ByteBuf> packets = new ObjectArrayList<>(count);
        try {
            int restoredBytes = 0;
            for (int i = 0; i < count; i++) {
                int size = BandwidthWire.readVarInt(raw);
                if (size <= 0 || size > settings.packetLimit() + 8 || size > raw.readableBytes()) {
                    throw new DecoderException("Invalid subpacket size " + size);
                }
                ByteBuf packet = incomingChannels.decode(allocator, raw.readSlice(size), incomingPayloadId, indexChannels);
                packets.add(packet);
                BandwidthWire.validatePacket(packet, incomingPayloadId, settings.payloadLimit(), settings.packetLimit());
                restoredBytes += packet.readableBytes() + BandwidthWire.varIntSize(packet.readableBytes());
                if (restoredBytes > settings.batchLimit()) {
                    throw new DecoderException("Restored packet batch exceeds configured limit");
                }
                if (BandwidthWire.envelope(packet, incomingPayloadId) != null) {
                    throw new DecoderException("Nested bandwidth envelope");
                }
                observeIncoming(packet);
            }
            if (raw.isReadable()) {
                throw new DecoderException("Trailing bytes in bandwidth batch");
            }
            return packets;
        } catch (Throwable failure) {
            packets.forEach(ByteBuf::release);
            throw failure;
        } finally {
            raw.release();
        }
    }

    public String statistics() {
        return "packets=" + sentPackets + ", indexed batch bytes=" + sentRawBytes + ", envelope bytes=" + sentEnvelopeBytes
            + ", channel header bytes saved=" + headerBytesSaved + ", dictionary version=" + dictionaryVersion;
    }

    void recordOutgoingBatch(int rawBytes, int wireBytes, int packets) {
        sendSequence++;
        sentRawBytes += rawBytes;
        sentEnvelopeBytes += wireBytes;
        sentPackets += packets;
        resetCompressionPending = false;
        resetOutgoingChannels = false;
        BandwidthStats.outgoingBatch(rawBytes, wireBytes, packets);
    }

    void discardUnsentBatch() {
        // ChannelIndex.encode and streaming compression are stateful. A candidate
        // that is not sent must not leave either state visible to the next batch.
        outgoingChannels.clear();
        resetOutgoingChannels = true;
        codec.resetCompressor();
        resetCompressionPending = true;
    }

    boolean handleControl(ByteBuf body) {
        if (!body.isReadable()) {
            throw new DecoderException("Empty Nova bandwidth control packet");
        }
        int operation = body.getUnsignedByte(body.readerIndex());
        if (operation == BandwidthWire.BATCH) {
            return false;
        }
        body.readUnsignedByte();
        switch (operation) {
            case BandwidthWire.DICTIONARY_REQUEST -> {
                int version = BandwidthWire.readVarInt(body);
                if (version < 0 || body.isReadable() || !server) {
                    throw new DecoderException("Invalid bandwidth dictionary request");
                }
                sendDictionaryData(true);
            }
            case BandwidthWire.DICTIONARY_DATA -> {
                int version = BandwidthWire.readVarInt(body);
                int size = BandwidthWire.readVarInt(body);
                if (version < 0 || size < 0 || size > BandwidthDictionary.MAX_DICTIONARY_BYTES || size > body.readableBytes()) {
                    throw new DecoderException("Invalid bandwidth dictionary size");
                }
                if (body.readableBytes() != size || server || version < dictionaryVersion) {
                    throw new DecoderException("Invalid bandwidth dictionary data");
                }
                byte[] dictionary = new byte[size];
                body.readBytes(dictionary);
                outbound.flushPending();
                installDictionary(new BandwidthDictionary.Snapshot(version, dictionary));
                dictionaryRequestSent = false;
                outbound.sendDictionaryReady(version);
                active = optimizationEnabled;
            }
            case BandwidthWire.DICTIONARY_READY -> {
                int version = BandwidthWire.readVarInt(body);
                if (version < 0 || body.isReadable() || !server) {
                    throw new DecoderException("Invalid bandwidth dictionary ready packet");
                }
                BandwidthDictionary.Snapshot latest = currentDictionary();
                BandwidthDictionary.Snapshot pending = pendingDictionary;
                if (pending != null && version != pending.version()) {
                    throw new DecoderException("Bandwidth dictionary ready does not match the pending version");
                }
                if (pending == null && version != dictionaryVersion) {
                    sendDictionaryData(true);
                } else {
                    if (pending != null) {
                        installDictionary(pending);
                        pendingDictionary = null;
                    }
                    latest = currentDictionary();
                    if (latest.version() != dictionaryVersion) {
                        sendDictionaryData(false);
                    } else {
                        active = optimizationEnabled;
                    }
                }
            }
            default -> throw new DecoderException("Unknown Nova bandwidth control operation " + operation);
        }
        return true;
    }

    void handleDictionaryMismatch(int version) {
        if (!dictionaryEnabled || closed) {
            throw new DictionaryMismatchException(version);
        }
        active = false;
        if (server) {
            // The peer may be using an older dictionary while this endpoint has
            // already installed the latest one. Force a resend even when the
            // server's current version equals its installed version.
            sendDictionaryData(true);
        } else if (!dictionaryRequestSent) {
            dictionaryRequestSent = true;
            outbound.sendDictionaryRequest(version);
        }
    }

    void observeOutgoing(ByteBuf packet) {
        if (server && dictionaryEnabled && BandwidthWire.envelope(packet, outgoingPayloadId) == null) {
            BandwidthDictionary.observe(packet);
        }
    }

    void refreshDictionary() {
        if (!server || !dictionaryEnabled || closed) {
            return;
        }
        BandwidthDictionary.Snapshot latest = BandwidthDictionary.snapshot();
        if (latest.version() != dictionaryVersion) {
            sendDictionaryData(false);
        }
    }

    void observeIncoming(ByteBuf packet) {
        if (server && dictionaryEnabled && BandwidthWire.envelope(packet, incomingPayloadId) == null) {
            BandwidthDictionary.observe(packet);
        }
    }

    private void sendDictionaryData(boolean force) {
        BandwidthDictionary.Snapshot latest = currentDictionary();
        // Serialize updates. A client must acknowledge one complete dictionary before
        // another can be sent, otherwise it would spend the whole connection one version behind.
        if (pendingDictionary != null) {
            return;
        }
        if (!force && latest.version() == dictionaryVersion) {
            return;
        }
        active = false;
        outbound.flushPending();
        pendingDictionary = latest;
        outbound.sendDictionaryData(latest.version(), latest.bytes());
    }

    private BandwidthDictionary.Snapshot currentDictionary() {
        return dictionaryEnabled ? BandwidthDictionary.snapshot() : new BandwidthDictionary.Snapshot(0, EMPTY_DICTIONARY);
    }

    private void installDictionary(BandwidthDictionary.Snapshot snapshot) {
        dictionaryVersion = snapshot.version();
        codec.loadDictionary(snapshot.bytes());
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            active = false;
            outbound.discard();
            codec.close();
            outgoingChannels.clear();
            incomingChannels.clear();
        }
    }

    static final class DictionaryMismatchException extends DecoderException {
        private final int version;

        DictionaryMismatchException(int version) {
            super("Unknown Nova bandwidth dictionary version " + version);
            this.version = version;
        }

        int version() {
            return version;
        }
    }
}
