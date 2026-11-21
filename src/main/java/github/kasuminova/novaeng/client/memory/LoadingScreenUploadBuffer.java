package github.kasuminova.novaeng.client.memory;

import org.lwjgl.BufferUtils;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

/**
 * Accessed with this class's monitor held for the complete upload.
 */
public final class LoadingScreenUploadBuffer {
    private static final int TARGET_PIXELS = 16 * 1024;
    private static IntBuffer pixels;
    private static int[] imagePixels;

    private LoadingScreenUploadBuffer() {
    }

    public static int pixelCapacity(final int width) {
        return Math.max(TARGET_PIXELS, width);
    }

    public static IntBuffer acquire(final int width) {
        final int capacity = pixelCapacity(width);
        if (pixels == null || pixels.capacity() < capacity) pixels = BufferUtils.createIntBuffer(capacity);
        if (imagePixels == null || imagePixels.length < capacity) imagePixels = new int[capacity];
        return pixels;
    }

    public static int rowsPerChunk(final int width) {
        return pixelCapacity(width) / width;
    }

    public static IntBuffer copyRows(final BufferedImage image, final int firstRow) {
        final int width = image.getWidth();
        final IntBuffer buffer = acquire(width);
        final int rows = Math.min(rowsPerChunk(width), image.getHeight() - firstRow);
        image.getRGB(0, firstRow, width, rows, imagePixels, 0, width);
        buffer.clear();
        buffer.put(imagePixels, 0, width * rows);
        buffer.flip();
        return buffer;
    }

    public static void release() {
        synchronized (LoadingScreenUploadBuffer.class) {
            pixels = null;
            imagePixels = null;
        }
    }
}
