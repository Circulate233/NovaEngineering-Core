package github.kasuminova.novaeng.common.nbt;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

/**
 * Reads large disk-backed NBT long arrays in fixed-size big-endian blocks.
 */
public final class NovaNbtLongArrayReader {
    private static final int BLOCK_BYTES = 8192;
    private static final int LONGS_PER_BLOCK = BLOCK_BYTES / Long.BYTES;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private NovaNbtLongArrayReader() {
    }

    public static boolean worthBulking(final int length) {
        return length > LONGS_PER_BLOCK;
    }

    public static void read(final DataInputStream input, final long[] target, final int length) throws IOException {
        final Scratch scratch = SCRATCH.get();
        int index = 0;
        while (index < length) {
            final int count = Math.min(LONGS_PER_BLOCK, length - index);
            input.readFully(scratch.bytes, 0, count * Long.BYTES);
            scratch.longBuffer.clear();
            scratch.longBuffer.limit(count);
            scratch.longBuffer.get(target, index, count);
            index += count;
        }
    }

    private static final class Scratch {
        private final byte[] bytes = new byte[BLOCK_BYTES];
        private final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
        private final LongBuffer longBuffer = byteBuffer.asLongBuffer();
    }
}
