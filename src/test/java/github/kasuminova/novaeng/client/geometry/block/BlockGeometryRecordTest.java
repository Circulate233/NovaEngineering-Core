package github.kasuminova.novaeng.client.geometry.block;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockGeometryRecordTest {
    static int[] rectangle(final int plane, final int[] order, final float fixed, final float a, final float b, final float c, final float d) {
        final int[] words = new int[28];
        for (int vertex = 0; vertex < 4; vertex++) {
            words[vertex * 7 + plane] = Float.floatToRawIntBits(fixed);
            words[vertex * 7 + (plane + 1) % 3] = Float.floatToRawIntBits((order[vertex] & 1) == 0 ? a : b);
            words[vertex * 7 + (plane + 2) % 3] = Float.floatToRawIntBits((order[vertex] & 2) == 0 ? c : d);
            words[vertex * 7 + 3] = -1;
            words[vertex * 7 + 4] = Float.floatToRawIntBits(vertex / 3F);
            words[vertex * 7 + 5] = Float.floatToRawIntBits(vertex / 7F);
            words[vertex * 7 + 6] = 0x007F0000;
        }
        return words;
    }

    @Test
    void allPlanesWindingAndRawMaterialWordsRoundTrip() {
        final Random random = new Random(0xFA_CE_BA_12L);
        final int[][] orders = {{0, 1, 3, 2}, {3, 1, 0, 2}, {1, 2, 0, 3}, {0, 2, 3, 1}};
        for (int plane = 0; plane < 3; plane++) {
            for (final int[] order : orders) {
                for (int trial = 0; trial < 100; trial++) {
                    final int[] words = rectangle(plane, order, trial / 16F, -2F, 7 / 16F, -3 / 16F, 2F);
                    for (int vertex = 0; vertex < 4; vertex++) {
                        for (int column = 3; column < 7; column++) words[vertex * 7 + column] = random.nextInt();
                    }
                    final BlockGeometryRecord record = BlockGeometryRecord.capture(words, 7, 0);
                    assertTrue(record.isRectangle());
                    assertArrayEquals(words, record.decode());
                }
            }
        }
    }

    @Test
    void offGridSignedZeroAndLargeCoordinatesKeepRawPositionBits() {
        for (final float fixed : new float[]{-0.0F, 0.1F, 65536F, Float.MIN_VALUE}) {
            final int[] words = rectangle(1, new int[]{3, 0, 2, 1}, fixed, -0.0F, 0.17F, -0.3F, 70000F);
            final BlockGeometryRecord record = BlockGeometryRecord.capture(words, 7, 0);
            assertTrue(record.isRectangle());
            assertArrayEquals(words, record.decode());
        }
    }

    @Test
    void shapeAndMaterialHaveIndependentContentKeys() {
        final int[] first = rectangle(2, new int[]{0, 1, 3, 2}, 1F, 0F, 1F, 0F, 1F);
        final int[] differentMaterial = first.clone();
        differentMaterial[4] = 0x7FA12345;
        final int[] differentShape = first.clone();
        for (int vertex = 0; vertex < 4; vertex++) differentShape[vertex * 7 + 2] = Float.floatToRawIntBits(0.5F);
        final BlockGeometryRecord a = BlockGeometryRecord.capture(first, 7, 0);
        final BlockGeometryRecord b = BlockGeometryRecord.capture(differentMaterial, 7, 0);
        final BlockGeometryRecord c = BlockGeometryRecord.capture(differentShape, 7, 0);
        assertEquals(a.shape, b.shape);
        assertNotEquals(a.material, b.material);
        assertNotEquals(a.shape, c.shape);
        assertEquals(a.material, c.material);
        assertEquals(a, BlockGeometryRecord.capture(first.clone(), 7, 0));
    }

    @Test
    void irregularAndUnknownFormatsRetainEveryRawWordAndOwnTheirSource() {
        final Random random = new Random(99);
        for (final int length : new int[]{0, 1, 27, 28, 36, 280}) {
            final int[] words = new int[length];
            for (int index = 0; index < length; index++) words[index] = random.nextInt();
            final int[] expected = words.clone();
            final BlockGeometryRecord record = BlockGeometryRecord.capture(words, 7, 0);
            assertFalse(record.isRectangle());
            if (length != 0) words[0] = 0;
            assertArrayEquals(expected, record.decode());
        }
        final int[] words = rectangle(0, new int[]{0, 1, 3, 2}, 0F, 0F, 1F, 0F, 1F);
        words[7] = 0x7FC12345;
        words[14] = 0xFFC54321;
        assertArrayEquals(words, BlockGeometryRecord.capture(words, 7, 0).decode());
    }

    @Test
    void nonzeroPositionOffsetAndMaxColumnMaskRoundTrip() {
        final int[] original = rectangle(0, new int[]{0, 2, 3, 1}, 0F, 0F, 1F, 0F, 1F);
        final Random random = new Random(77);
        final int[] words = new int[66 * 4];
        for (int index = 0; index < words.length; index++) words[index] = random.nextInt();
        for (int vertex = 0; vertex < 4; vertex++) System.arraycopy(original, vertex * 7, words, vertex * 66 + 61, 3);
        final BlockGeometryRecord record = BlockGeometryRecord.capture(words, 66, 61);
        assertTrue(record.isRectangle());
        assertArrayEquals(words, record.decode());
    }

    @Test
    void everySupportedStrideAndPositionPreserveUninterpretedWords() {
        final Random random = new Random(0x11_19);
        for (int stride = 3; stride <= 66; stride++)
            for (int position = 0; position <= stride - 3; position++) {
                int[] rect = rectangle(stride % 3, new int[]{2, 0, 1, 3}, -0.0F, 0.1F, 0.25F, -24F, 7F);
                final int[] words = new int[4 * stride];
                for (int d = 0; d < stride; d++) {
                    int constant = random.nextInt();
                    for (int v = 0; v < 4; v++) words[v * stride + d] = (d % 3 == 0) ? constant : random.nextInt();
                }
                for (int v = 0; v < 4; v++) System.arraycopy(rect, 7 * v, words, stride * v + position, 3);
                assertArrayEquals(words, BlockGeometryRecord.capture(words, stride, position).decode());
                words[position] = 0x7fc10101;
                assertArrayEquals(words, BlockGeometryRecord.capture(words, stride, position).decode());
            }
    }
}
