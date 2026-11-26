package github.kasuminova.novaeng.client.memory;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RenderMemoryOptimizationTest {
    @Test
    void smallInitialBufferGrowsAndPreservesEveryVertexWord() {
        final BufferBuilder builder = new BufferBuilder(RenderBufferCapacity.initialInts(2097152));
        assertEquals(64 * 1024, builder.getByteBuffer().capacity());
        builder.begin(7, DefaultVertexFormats.POSITION);
        final int[] words = new int[3 * 24000];
        final Random random = new Random(20261119L);
        for (int i = 0; i < words.length; i++) words[i] = random.nextInt();
        builder.addVertexData(words);
        builder.finishDrawing();
        assertEquals(words.length / 3, builder.getVertexCount());
        final IntBuffer contents = builder.getByteBuffer().asIntBuffer();
        assertEquals(words.length, contents.remaining());
        final int[] copied = new int[words.length];
        contents.get(copied);
        assertArrayEquals(words, copied);
        assertEquals(32, RenderBufferCapacity.initialInts(32), "Keep intentionally tiny caller buffers tiny");
    }

    @Test
    void chunkedUploadsPreserveOddSizedAndVeryWideImagesAndReuseStorage() throws Exception {
        synchronized (LoadingScreenUploadBuffer.class) {
            try {
                for (final int width : new int[]{1, 7, 333, 16384, 16385, 20000}) {
                    final int height = width < 1000 ? 137 : 3;
                    final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    final int[] expected = new int[width * height];
                    for (int i = 0; i < expected.length; i++) expected[i] = i * 0x31415927;
                    image.setRGB(0, 0, width, height, expected, 0, width);
                    final int[] actual = new int[expected.length];
                    IntBuffer previous = null;
                    int output = 0;
                    for (int row = 0; row < height; row += LoadingScreenUploadBuffer.rowsPerChunk(width)) {
                        final IntBuffer chunk = LoadingScreenUploadBuffer.copyRows(image, row);
                        if (previous != null) assertSame(previous, chunk);
                        assertEquals(0, chunk.position());
                        assertEquals(0, chunk.remaining() % width);
                        final int count = chunk.remaining();
                        chunk.get(actual, output, count);
                        output += count;
                        previous = chunk;
                    }
                    assertEquals(expected.length, output);
                    assertArrayEquals(expected, actual);
                }
            } finally {
                LoadingScreenUploadBuffer.release();
            }
            for (final String fieldName : new String[]{"pixels", "imagePixels"}) {
                final var field = LoadingScreenUploadBuffer.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                assertNull(field.get(null), "No image or direct storage survives loading-screen close");
            }
        }
    }
}
