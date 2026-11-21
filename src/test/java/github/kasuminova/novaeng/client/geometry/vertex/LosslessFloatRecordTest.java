package github.kasuminova.novaeng.client.geometry.vertex;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class LosslessFloatRecordTest {
    static IVertexConsumer consumer(final VertexFormat format, final List<float[]> retained) {
        return new IVertexConsumer() {
            @Override
            public VertexFormat getVertexFormat() {
                return format;
            }

            @Override
            public void setQuadTint(final int tint) {
            }

            @Override
            public void setQuadOrientation(final EnumFacing face) {
            }

            @Override
            public void setTexture(final TextureAtlasSprite sprite) {
            }

            @Override
            public void setApplyDiffuseLighting(final boolean diffuse) {
            }

            @Override
            public void put(final int element, final float... data) {
                retained.add(data);
            }
        };
    }

    static float[][][] tree(final BakedQuad quad) throws Exception {
        final Field field = UnpackedBakedQuad.class.getDeclaredField("unpackedData");
        field.setAccessible(true);
        return (float[][][]) field.get(quad);
    }

    static void assertTreeEquals(final float[][][] expected, final float[][][] actual) {
        assertEquals(expected.length, actual.length);
        for (int v = 0; v < expected.length; v++) {
            assertEquals(expected[v].length, actual[v].length);
            for (int e = 0; e < expected[v].length; e++)
                for (int c = 0; c < 4; c++) {
                    assertEquals(Float.floatToRawIntBits(expected[v][e][c]), Float.floatToRawIntBits(actual[v][e][c]),
                        "v=" + v + " e=" + e + " c=" + c);
                }
        }
    }

    @Test
    void arbitraryRawWordsAndAllFourComponentsRoundTrip() {
        final Random random = new Random(0x4c4f53534c455353L);
        for (int elements = 0; elements <= 16; elements++) {
            for (int sample = 0; sample < 50; sample++) {
                final float[] flat = new float[elements * 16];
                for (int i = 0; i < flat.length; i++) flat[i] = Float.intBitsToFloat(random.nextInt());
                final LosslessFloatRecord record = LosslessFloatRecord.capture(flat, elements);
                final float[][][] tree = record.materialize();
                for (int v = 0; v < 4; v++)
                    for (int e = 0; e < elements; e++)
                        for (int c = 0; c < 4; c++) {
                            final int bits = Float.floatToRawIntBits(flat[((v * elements + e) << 2) + c]);
                            assertEquals(bits, record.rawBits(v, e, c));
                            assertEquals(bits, Float.floatToRawIntBits(tree[v][e][c]));
                        }
                assertEquals(record, LosslessFloatRecord.capture(flat, elements));
                assertEquals(record.hashCode(), LosslessFloatRecord.capture(flat, elements).hashCode());
                if (elements > 0) flat[0] = 42;
                // The record owns its data and cannot be changed through either input or expansion.
                if (elements > 0) assertNotEquals(Float.floatToRawIntBits(flat[0]), record.rawBits(0, 0, 0));
            }
        }
    }

    @Test
    void constantsSignedZeroAndNanPayloadsUseActualBits() {
        final int elements = 5;
        final float[] flat = new float[elements * 16];
        for (int v = 0; v < 4; v++) {
            flat[v * elements * 4] = -0.0f;
            flat[v * elements * 4 + 1] = Float.intBitsToFloat(0x7fc00001);
            flat[v * elements * 4 + 2] = Float.intBitsToFloat(0x7fc00010 + v);
        }
        final LosslessFloatRecord record = LosslessFloatRecord.capture(flat, elements);
        assertFalse(record.isRaw());
        assertEquals(6, record.storedWordCount());
        assertEquals(0x80000000, record.rawBits(2, 0, 0));
        assertEquals(0x7fc00001, record.rawBits(3, 0, 1));
        assertEquals(0x7fc00013, record.rawBits(3, 0, 2));
        assertEquals(0, record.rawBits(1, 4, 3));
    }

    @Test
    void compactOffsetsCoverEveryChannelAndExposedTreesSnapshotWithoutAnIntermediateFlatCopy() {
        final Random random = new Random(0x20_11_2026L);
        for (int elements = 1; elements <= 16; elements++) {
            final FlatUnpackedData data = new FlatUnpackedData(elements);
            final float[] flat = new float[elements * 16];
            for (int v = 0; v < 4; v++)
                for (int e = 0; e < elements; e++) {
                    final float[] values = new float[4];
                    for (int c = 0; c < 4; c++) {
                        final int channel = e * 4 + c;
                        values[c] = switch (channel % 3) {
                            case 0 -> 0F;
                            case 1 -> Float.intBitsToFloat(0x80000000 | channel);
                            default -> Float.intBitsToFloat(random.nextInt());
                        };
                        flat[(v * elements + e) * 4 + c] = values[c];
                    }
                    data.put(v, e, values);
                }
            final LosslessFloatRecord expected = LosslessFloatRecord.capture(flat, elements);
            assertEquals(expected, data.snapshot());
            final float[][][] exposed = data.materialize();
            final LosslessFloatRecord snapshot = data.snapshot();
            assertEquals(expected, snapshot);
            for (int v = 0; v < 4; v++)
                for (int e = 0; e < elements; e++)
                    for (int c = 0; c < 4; c++)
                        assertEquals(Float.floatToRawIntBits(flat[(v * elements + e) * 4 + c]), snapshot.rawBits(v, e, c));
            final int before = snapshot.rawBits(0, 0, 0);
            exposed[0][0] = new float[]{1F, 2F, 3F, 4F, 5F};
            assertEquals(before, snapshot.rawBits(0, 0, 0));
            assertEquals(Float.floatToRawIntBits(4F), data.snapshot().rawBits(0, 0, 3));
        }
    }

    @Test
    void flatDataMatchesOriginalBuilderForSparseRepeatedAndOutOfOrderPuts() throws Exception {
        final Random random = new Random(1896);
        final Sprite sprite = new Sprite();
        for (final VertexFormat format : List.of(DefaultVertexFormats.ITEM, DefaultVertexFormats.BLOCK)) {
            final int count = format.getElementCount();
            for (int sample = 0; sample < 100; sample++) {
                final FlatUnpackedData flat = new FlatUnpackedData(count);
                final UnpackedBakedQuad.Builder original = new UnpackedBakedQuad.Builder(format);
                original.setTexture(sprite);
                original.setQuadOrientation(EnumFacing.EAST);
                original.setQuadTint(3);
                original.setApplyDiffuseLighting(false);
                for (int v = 0; v < 4; v++)
                    for (int e = 0; e < count; e++) {
                        final int target = sample % 3 == 0 ? random.nextInt(count) : count - e - 1;
                        final float[] values = new float[random.nextInt(7)];
                        for (int c = 0; c < values.length; c++) values[c] = Float.intBitsToFloat(random.nextInt());
                        original.put(target, values);
                        flat.put(v, target, values);
                    }
                final UnpackedBakedQuad reference = original.build();
                assertTreeEquals(tree(reference), flat.materialize());
                final OwnedUnpackedQuadRecord record = new OwnedUnpackedQuadRecord(flat.snapshot(), 3,
                    EnumFacing.EAST, sprite, false, format);
                assertArrayEquals(reference.getVertexData(), record.packed());
                final int[] reused = new int[format.getSize()];
                Arrays.fill(reused, -1);
                record.packInto(reused);
                assertArrayEquals(reference.getVertexData(), reused);
                final UnpackedBakedQuad expanded = record.materialize();
                assertTreeEquals(tree(reference), tree(expanded));
                assertArrayEquals(reference.getVertexData(), expanded.getVertexData());
                assertEquals(3, expanded.getTintIndex());
                assertEquals(EnumFacing.EAST, expanded.getFace());
                assertSame(sprite, expanded.getSprite());
                assertSame(format, expanded.getFormat());
                assertFalse(expanded.shouldApplyDiffuseLighting());
            }
        }
    }

    @Test
    void uvContractionAndRepeatedBuildUseForgeOperationOrder() throws Exception {
        final Random random = new Random(0x555643);
        final Sprite sprite = new Sprite();
        final VertexFormat format = DefaultVertexFormats.ITEM;
        final int count = format.getElementCount();
        for (int sample = 0; sample < 100; sample++) {
            final FlatUnpackedData flat = new FlatUnpackedData(count);
            final UnpackedBakedQuad.Builder original = new UnpackedBakedQuad.Builder(format);
            original.setTexture(sprite);
            original.setContractUVs(true);
            for (int v = 0; v < 4; v++)
                for (int e = 0; e < count; e++) {
                    final float[] value = {random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat()};
                    original.put(e, value);
                    flat.put(v, e, value);
                }
            for (int build = 0; build < 5; build++) {
                final UnpackedBakedQuad reference = original.build();
                flat.contractUVs(format, sprite);
                assertTreeEquals(tree(reference), flat.snapshot().materialize());
                if (build == 2) {
                    // Both aliases remain live after the public boundary, including pipe array edits.
                    final float[][][] originalTree = tree(reference);
                    final float[][][] expanded = flat.materialize();
                    originalTree[0][0][3] = expanded[0][0][3] = -0.0f;
                }
            }
        }
    }

    @Test
    void realMaterializationPreservesIndependentFloatAndLazyPackedSources() {
        final VertexFormat format = DefaultVertexFormats.ITEM;
        final FlatUnpackedData flat = new FlatUnpackedData(format.getElementCount());
        for (int v = 0; v < 4; v++)
            for (int e = 0; e < format.getElementCount(); e++) {
                flat.put(v, e, new float[]{v, e, 0, 1});
            }
        final UnpackedBakedQuad actual = new OwnedUnpackedQuadRecord(flat.snapshot(), -1,
            EnumFacing.NORTH, new Sprite(), true, format).materialize();
        final List<float[]> retained = new ArrayList<>();
        actual.pipe(consumer(format, retained));
        retained.getFirst()[0] = 0.75f;
        final int[] packed = actual.getVertexData();
        assertEquals(Float.floatToRawIntBits(0.75f), packed[0]);
        retained.getFirst()[0] = 0.5f;
        assertSame(packed, actual.getVertexData());
        assertEquals(Float.floatToRawIntBits(0.75f), packed[0]);
        packed[0] = Float.floatToRawIntBits(0.125f);
        final List<float[]> second = new ArrayList<>();
        actual.pipe(consumer(format, second));
        assertSame(retained.getFirst(), second.getFirst());
        assertEquals(0.5f, second.getFirst()[0]);
        assertEquals(Float.floatToRawIntBits(0.125f), actual.vertexData[0]);
    }

    @Test
    void trustedFluidListProvidesCompleteEphemeralPackedQuadsAndExactFloatPipe() {
        final VertexFormat format = DefaultVertexFormats.ITEM;
        final FlatUnpackedData flat = new FlatUnpackedData(format.getElementCount());
        for (int v = 0; v < 4; v++)
            for (int e = 0; e < format.getElementCount(); e++) {
                flat.put(v, e, new float[]{v * 0.125f, e * 0.25f, -0.0f, Float.intBitsToFloat(0x7fc00059)});
            }
        final Sprite sprite = new Sprite();
        final OwnedUnpackedQuadRecord record = new OwnedUnpackedQuadRecord(flat.snapshot(), 7,
            EnumFacing.WEST, sprite, false, format);
        final OwnedFluidQuadList list = new OwnedFluidQuadList(List.of(new OwnedUnpackedQuadView(record)));
        final BakedQuad read = list.getFirst();
        assertFalse(read instanceof OwnedUnpackedQuadView);
        assertNotSame(read, list.getFirst());
        assertArrayEquals(record.packed(), read.vertexData);
        assertSame(read.vertexData, read.getVertexData());
        assertSame(format, read.getFormat());
        assertSame(sprite, read.getSprite());
        assertEquals(7, read.getTintIndex());
        assertEquals(EnumFacing.WEST, read.getFace());
        assertFalse(read.shouldApplyDiffuseLighting());
        final List<float[]> first = new ArrayList<>();
        read.pipe(consumer(format, first));
        assertEquals(0x7fc00059, Float.floatToRawIntBits(first.getFirst()[3]));
        first.getFirst()[0] = 0.75f;
        final List<float[]> second = new ArrayList<>();
        read.pipe(consumer(format, second));
        assertSame(first.getFirst(), second.getFirst());
        assertEquals(0.75f, second.getFirst()[0]);
    }

    @Test
    void metadataInterningRetainsFormatAndSpriteIdentity() {
        final LosslessFloatRecord data = LosslessFloatRecord.capture(new float[80], 5);
        final VertexFormat firstFormat = new VertexFormat(DefaultVertexFormats.ITEM);
        final VertexFormat secondFormat = new VertexFormat(DefaultVertexFormats.ITEM);
        assertEquals(firstFormat, secondFormat);
        final Sprite sprite = new Sprite();
        final OwnedUnpackedQuadRecord first = new OwnedUnpackedQuadRecord(data, 0, EnumFacing.UP, sprite, true, firstFormat);
        final OwnedUnpackedQuadRecord second = new OwnedUnpackedQuadRecord(data, 0, EnumFacing.UP, sprite, true, secondFormat);
        assertNotEquals(first, second);
    }

    static final class Sprite extends TextureAtlasSprite {
        Sprite() {
            super("novaeng:lossless-test");
            setIconWidth(16);
            setIconHeight(32);
            initSprite(4096, 8192, 177, 275, false);
        }
    }
}
