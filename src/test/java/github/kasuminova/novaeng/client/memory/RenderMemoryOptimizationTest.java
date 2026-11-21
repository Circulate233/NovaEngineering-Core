package github.kasuminova.novaeng.client.memory;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.awt.image.BufferedImage;
import java.nio.IntBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RenderMemoryOptimizationTest {
    @Test
    void chunkPixelTransferMatchesTheInstalledAnaglyphFunctionWithoutChangingSource() throws Exception {
        final ClassNode original = new ClassNode();
        try (final var input = getClass().getResourceAsStream("/net/minecraft/client/renderer/texture/TextureUtil.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(original, 0);
        }
        final var colorMethod = original.methods.stream()
                                                .filter(method -> method.desc.equals("(I)I") && (method.name.equals("anaglyphColor") || method.name.equals("func_177054_c")))
                                                .findFirst().orElseThrow();
        final ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "AnaglyphReference", null, "java/lang/Object", null);
        colorMethod.accept(writer);
        writer.visitEnd();
        final Class<?> reference = new ClassLoader(getClass().getClassLoader()) {
            Class<?> define() {
                return defineClass("AnaglyphReference", writer.toByteArray(), 0, writer.toByteArray().length);
            }
        }.define();
        final var convert = reference.getMethod(colorMethod.name, int.class);
        final int[] source = new int[50003];
        final Random random = new Random(0xA11A61F);
        for (int i = 0; i < source.length; i++) source[i] = random.nextInt();
        final int[] unchanged = source.clone();
        final IntBuffer target = IntBuffer.allocate(10000);
        for (final boolean anaglyph : new boolean[]{false, true}) {
            for (int offset = 3; offset < source.length; offset += target.capacity()) {
                final int count = Math.min(target.capacity(), source.length - offset);
                TexturePixelTransfer.copy(target, source, offset, count, anaglyph);
                assertEquals(0, target.position());
                assertEquals(count, target.limit());
                for (int i = 0; i < count; i++) {
                    assertEquals(anaglyph ? (int) convert.invoke(null, source[offset + i]) : source[offset + i], target.get());
                }
            }
        }
        assertArrayEquals(unchanged, source);
    }

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
