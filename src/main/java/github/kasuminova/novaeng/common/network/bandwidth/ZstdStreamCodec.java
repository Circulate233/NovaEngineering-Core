package github.kasuminova.novaeng.common.network.bandwidth;

import com.github.luben.zstd.EndDirective;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

import java.nio.ByteBuffer;

/** Connection-owned native contexts, used and closed only on the channel event loop. */
public final class ZstdStreamCodec implements AutoCloseable {
    private final BandwidthSettings settings;
    private ZstdCompressCtx compressor;
    private ZstdDecompressCtx decompressor;
    private boolean atFrameStart = true;
    private byte[] dictionary = new byte[0];

    public ZstdStreamCodec(BandwidthSettings settings) {
        this.settings = settings;
    }

    public ByteBuf compress(ByteBufAllocator allocator, ByteBuf input) {
        if (compressor == null) {
            compressor = new ZstdCompressCtx();
            compressor.setLevel(3).setContentSize(false).setWindowLog(settings.windowLog());
            if (dictionary.length != 0) {
                compressor.loadDict(dictionary);
            }
        }
        // Include room for flushing a partial streaming block as well as the normal compressBound.
        int capacity = Math.toIntExact(Zstd.compressBound(input.readableBytes()) + 128 * 1024);
        ByteBuf result = allocator.directBuffer(capacity, capacity);
        ByteBuf direct = input.isDirect() ? input.retain() : allocator.directBuffer(input.readableBytes()).writeBytes(input, input.readerIndex(), input.readableBytes());
        try {
            ByteBuffer source = direct.nioBuffer();
            ByteBuffer target = result.nioBuffer(0, capacity);
            boolean done;
            do {
                int beforeInput = source.position();
                int beforeOutput = target.position();
                done = compressor.compressDirectByteBufferStream(target, source,
                    settings.reuseContext() ? EndDirective.FLUSH : EndDirective.END);
                if (!done && source.position() == beforeInput && target.position() == beforeOutput) {
                    throw new EncoderException("Zstd made no progress within the bounded output buffer");
                }
            } while (!done);
            if (source.hasRemaining()) {
                throw new EncoderException("Zstd did not consume the entire batch");
            }
            result.writerIndex(target.position());
            return result;
        } catch (Throwable failure) {
            result.release();
            throw failure;
        } finally {
            direct.release();
        }
    }

    public ByteBuf decompress(ByteBufAllocator allocator, ByteBuf input, int size) {
        if (size <= 0 || size > settings.batchLimit()) {
            throw new DecoderException("Invalid uncompressed batch size " + size);
        }
        if (decompressor == null) {
            decompressor = new ZstdDecompressCtx();
            if (dictionary.length != 0) {
                decompressor.loadDict(dictionary);
            }
        }
        if (atFrameStart) {
            validateWindow(input);
        }
        ByteBuf result = allocator.directBuffer(size, size);
        ByteBuf direct = input.isDirect() ? input.retain() : allocator.directBuffer(input.readableBytes()).writeBytes(input, input.readerIndex(), input.readableBytes());
        try {
            ByteBuffer source = direct.nioBuffer();
            ByteBuffer target = result.nioBuffer(0, size);
            while (source.hasRemaining()) {
                int beforeInput = source.position();
                int beforeOutput = target.position();
                atFrameStart = decompressor.decompressDirectByteBufferStream(target, source);
                if (atFrameStart && source.hasRemaining()) {
                    throw new DecoderException("Multiple Zstd frames in one batch");
                }
                if (source.position() == beforeInput && target.position() == beforeOutput) {
                    throw new DecoderException("Zstd batch exceeds its declared uncompressed size or is truncated");
                }
            }
            if (target.position() != size) {
                throw new DecoderException("Zstd size mismatch: expected " + size + ", got " + target.position());
            }
            result.writerIndex(size);
            return result;
        } catch (Throwable failure) {
            result.release();
            throw failure;
        } finally {
            direct.release();
        }
    }

    private static void validateWindow(ByteBuf input) {
        int start = input.readerIndex();
        // Our streaming encoder disables content-size fields and therefore single-segment frames.
        // Validate the window descriptor before zstd allocates its history (JNI 1.5.7-4 has no ctx setter).
        if (input.readableBytes() < 6 || input.getIntLE(start) != 0xFD2FB528 || (input.getUnsignedByte(start + 4) & 0x20) != 0) {
            throw new DecoderException("Invalid streaming Zstd frame header");
        }
        int descriptor = input.getUnsignedByte(start + 5);
        long base = 1L << (10 + (descriptor >>> 3));
        long window = base + (base >>> 3) * (descriptor & 7);
        if (window > 32L * BandwidthSettings.MIB) {
            throw new DecoderException("Zstd history window exceeds 32 MiB");
        }
    }

    public void loadDictionary(byte[] dictionary) {
        if (dictionary == null || dictionary.length > BandwidthDictionary.MAX_DICTIONARY_BYTES) {
            throw new IllegalArgumentException("Invalid bandwidth dictionary");
        }
        if (this.dictionary == dictionary) {
            return;
        }
        if (compressor != null) {
            compressor.close();
            compressor = null;
        }
        if (decompressor != null) {
            decompressor.close();
            decompressor = null;
        }
        this.dictionary = dictionary;
        atFrameStart = true;
    }

    void resetCompressor() {
        if (compressor != null) {
            compressor.close();
            compressor = null;
        }
    }

    void resetDecompressor() {
        if (decompressor != null) {
            decompressor.close();
            decompressor = null;
        }
        atFrameStart = true;
    }

    @Override
    public void close() {
        resetCompressor();
        resetDecompressor();
    }
}
