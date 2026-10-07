package github.kasuminova.novaeng.client.font;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/** Reuses the tiny direct buffers used to query GL state while drawing text. */
public final class FontGlStateScratch {
    private static final ThreadLocal<FloatBuffer> FLOATS = ThreadLocal.withInitial(() -> BufferUtils.createFloatBuffer(16));
    private static final ThreadLocal<ByteBuffer> BYTES = ThreadLocal.withInitial(() -> BufferUtils.createByteBuffer(4));

    private FontGlStateScratch() {
    }

    public static FloatBuffer floats(final int size) {
        final FloatBuffer buffer = FLOATS.get();
        buffer.clear();
        buffer.limit(size);
        return buffer;
    }

    public static ByteBuffer bytes(final int size) {
        final ByteBuffer buffer = BYTES.get();
        buffer.clear();
        buffer.limit(size);
        return buffer;
    }
}
