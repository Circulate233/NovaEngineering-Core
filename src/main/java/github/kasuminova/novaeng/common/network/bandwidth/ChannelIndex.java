package github.kasuminova.novaeng.common.network.bandwidth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.DecoderException;

import java.nio.charset.StandardCharsets;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

/** Separate instances are used for sending and receiving. Definitions precede every reference when enabled. */
public final class ChannelIndex {
    public static final int MAX_CHANNELS = 4096;
    private final Object2IntOpenHashMap<String> outbound = new Object2IntOpenHashMap<>();
    private final ObjectList<byte[]> inbound = new ObjectArrayList<>();

    public ChannelIndex() {
        outbound.defaultReturnValue(0);
    }

    public void clear() {
        outbound.clear();
        inbound.clear();
    }

    public ByteBuf encode(ByteBufAllocator allocator, ByteBuf packet, int payloadId, boolean enabled) {
        if (!enabled) {
            return packet.retain();
        }
        ByteBuf view = packet.duplicate();
        if (BandwidthWire.readVarInt(view) != payloadId) {
            return packet.retain();
        }
        int nameLength = BandwidthWire.readVarInt(view);
        if (nameLength < 0 || nameLength > 80 || nameLength > view.readableBytes()) {
            throw new DecoderException("Invalid channel length");
        }
        byte[] name = new byte[nameLength];
        view.readBytes(name);
        String key = new String(name, StandardCharsets.UTF_8);
        int id = outbound.getInt(key);
        boolean define = id == 0 && outbound.size() < MAX_CHANNELS;
        if (define) {
            id = outbound.size() + 1;
            outbound.put(key, id);
        }
        ByteBuf result = allocator.buffer(packet.readableBytes() + 8);
        try {
            BandwidthWire.writeVarInt(result, payloadId);
            BandwidthWire.writeVarInt(result, id == 0 ? 0 : (id << 1) | (define ? 1 : 0));
            if (id == 0 || define) {
                BandwidthWire.writeVarInt(result, name.length);
                result.writeBytes(name);
            }
            result.writeBytes(view);
            return result;
        } catch (Throwable failure) {
            result.release();
            throw failure;
        }
    }

    public ByteBuf decode(ByteBufAllocator allocator, ByteBuf packet, int payloadId, boolean enabled) {
        if (!enabled) {
            return packet.retain();
        }
        ByteBuf view = packet.duplicate();
        if (BandwidthWire.readVarInt(view) != payloadId) {
            return packet.retain();
        }
        int token = BandwidthWire.readVarInt(view);
        if (token < 0 || token > (MAX_CHANNELS << 1) + 1 || token == 1) {
            throw new DecoderException("Invalid channel index token " + token);
        }
        int id = token >>> 1;
        byte[] name;
        if (token == 0 || (token & 1) != 0) {
            if (token != 0 && id != inbound.size() + 1) {
                throw new DecoderException("Out-of-order or duplicate channel definition " + id);
            }
            int length = BandwidthWire.readVarInt(view);
            if (length < 0 || length > 80 || length > view.readableBytes()) {
                throw new DecoderException("Invalid indexed channel name length");
            }
            name = new byte[length];
            view.readBytes(name);
            if (token != 0) {
                inbound.add(name);
            }
        } else {
            if (id == 0 || id > inbound.size()) {
                throw new DecoderException("Undefined channel index " + id);
            }
            name = inbound.get(id - 1);
        }
        ByteBuf result = allocator.buffer(view.readableBytes() + name.length + 10);
        try {
            BandwidthWire.writeVarInt(result, payloadId);
            BandwidthWire.writeVarInt(result, name.length);
            result.writeBytes(name).writeBytes(view);
            return result;
        } catch (Throwable failure) {
            result.release();
            throw failure;
        }
    }
}
