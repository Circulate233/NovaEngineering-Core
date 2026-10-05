package github.kasuminova.novaeng.common.network.bandwidth;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Versioned envelopes use an ordinary 1.12.2 CustomPayload channel. */
public final class BandwidthWire {
    public static final String CHANNEL = "novaeng|bw";
    public static final int VERSION = 3;
    public static final int BATCH = 3;
    public static final int DICTIONARY_REQUEST = 4;
    public static final int DICTIONARY_DATA = 5;
    public static final int DICTIONARY_READY = 6;
    private static final byte[] CHANNEL_BYTES = CHANNEL.getBytes(StandardCharsets.UTF_8);

    private BandwidthWire() {
    }

    public static int readVarInt(ByteBuf in) {
        int value = 0;
        for (int i = 0; i < 5; i++) {
            int next = in.readUnsignedByte();
            if (i == 4 && (next & 0xF0) != 0) {
                throw new DecoderException("VarInt overflow");
            }
            value |= (next & 0x7F) << (i * 7);
            if ((next & 0x80) == 0) {
                return value;
            }
        }
        throw new DecoderException("VarInt exceeds five bytes");
    }

    public static void writeVarInt(ByteBuf out, int value) {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    public static int varIntSize(int value) {
        int size = 1;
        while ((value & ~0x7F) != 0) {
            size++;
            value >>>= 7;
        }
        return size;
    }

    /** Returns a non-owning view positioned after the channel, or null for a different packet. */
    public static ByteBuf envelope(ByteBuf packet, int customPayloadId) {
        ByteBuf view = packet.duplicate();
        if (readVarInt(view) != customPayloadId || readVarInt(view) != CHANNEL_BYTES.length) {
            return null;
        }
        if (view.readableBytes() < CHANNEL_BYTES.length) {
            return null;
        }
        for (byte expected : CHANNEL_BYTES) {
            if (view.readByte() != expected) {
                return null;
            }
        }
        return view;
    }

    public static void header(ByteBuf out, int customPayloadId, int operation) {
        writeVarInt(out, customPayloadId);
        writeVarInt(out, CHANNEL_BYTES.length);
        out.writeBytes(CHANNEL_BYTES);
        out.writeByte(operation);
    }

    public static void dictionaryRequest(ByteBuf out, int customPayloadId, int version) {
        header(out, customPayloadId, DICTIONARY_REQUEST);
        writeVarInt(out, version);
    }

    public static void dictionaryData(ByteBuf out, int customPayloadId, int version, byte[] dictionary) {
        header(out, customPayloadId, DICTIONARY_DATA);
        writeVarInt(out, version);
        writeVarInt(out, dictionary.length);
        out.writeBytes(dictionary);
    }

    public static void dictionaryReady(ByteBuf out, int customPayloadId, int version) {
        header(out, customPayloadId, DICTIONARY_READY);
        writeVarInt(out, version);
    }

    public static String customChannel(ByteBuf packet, int customPayloadId) {
        ByteBuf view = packet.duplicate();
        if (readVarInt(view) != customPayloadId) {
            return null;
        }
        int size = readVarInt(view);
        if (size < 0 || size > 80 || size > view.readableBytes()) {
            throw new DecoderException("Invalid CustomPayload channel length");
        }
        return view.readCharSequence(size, StandardCharsets.UTF_8).toString();
    }

    public static void validatePacket(ByteBuf packet, int customPayloadId, int payloadLimit, int packetLimit) {
        if (packet.readableBytes() == 0 || packet.readableBytes() > packetLimit) {
            throw new DecoderException("Packet exceeds configured limit " + packetLimit);
        }
        ByteBuf view = packet.duplicate();
        if (readVarInt(view) == customPayloadId) {
            int channelLength = readVarInt(view);
            if (channelLength < 0 || channelLength > 80 || channelLength > view.readableBytes()) {
                throw new DecoderException("Invalid CustomPayload channel length");
            }
            view.skipBytes(channelLength);
            if (view.readableBytes() > payloadLimit) {
                throw new DecoderException("CustomPayload exceeds configured limit " + payloadLimit);
            }
        }
    }

    /** Decode at most one frame; preserve the cursor until a complete frame is available. */
    public static void decodeFrame(ByteBuf in, List<Object> out, int limit) {
        int start = in.readerIndex();
        int length = 0;
        for (int i = 0; i < 4; i++) {
            if (!in.isReadable()) {
                in.readerIndex(start);
                return;
            }
            int next = in.readUnsignedByte();
            length |= (next & 0x7F) << (i * 7);
            if ((next & 0x80) == 0) {
                if (length <= 0 || length > limit) {
                    throw new DecoderException("Frame length " + length + " exceeds permitted range 1.." + limit);
                }
                if (in.readableBytes() < length) {
                    in.readerIndex(start);
                    return;
                }
                out.add(in.readRetainedSlice(length));
                return;
            }
        }
        throw new DecoderException("Frame length exceeds four bytes");
    }

    public static void encodeFrame(ByteBuf packet, ByteBuf out, int limit) {
        int length = packet.readableBytes();
        if (length <= 0 || length > limit) {
            throw new EncoderException("Frame length " + length + " exceeds permitted range 1.." + limit);
        }
        writeVarInt(out, length);
        out.writeBytes(packet, packet.readerIndex(), length);
    }
}
