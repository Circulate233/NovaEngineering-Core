package github.kasuminova.novaeng.client.geometry.block;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockGeometryPagesTest {
    @Test
    void capturingGeometryAndMaterialsDoesNotWaitForAnUnrelatedPageBind() throws Exception {
        final BlockGeometryPages pages = new BlockGeometryPages(1);
        final var writers = BlockGeometryPages.class.getDeclaredField("writers");
        writers.setAccessible(true);
        final Object writer = ((Object[]) writers.get(pages))[0];
        final int[] words = BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 0F, 0F, 1F, 0F, 1F);
        final BakedQuad template = new BakedQuad(words, 7, EnumFacing.SOUTH, null, true, DefaultVertexFormats.ITEM);
        try (final var executor = Executors.newSingleThreadExecutor()) {
            // Simulate another worker binding page IDs on this stripe. Capture
            // and material equality only use their own immutable records/indices.
            synchronized (writer) {
                final var encoded = executor.submit(() -> {
                    final var record = pages.capture(words, 7, 0);
                    final var material = pages.material(record.material, template);
                    return material.quad(record.pageShape());
                }).get(10, TimeUnit.SECONDS);
                assertArrayEquals(words, encoded.getVertexData());
                assertEquals(7, encoded.getTintIndex());
            }
        }
        pages.seal();
    }

    @Test
    void parallelEncodersCanCrossTheCloseBarrierWithoutRetainingOrChangingGeometry() throws Exception {
        final BlockGeometryPages pages = new BlockGeometryPages(1);
        final CountDownLatch started = new CountDownLatch(4);
        final CountDownLatch crossBarrier = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(4)) {
            final var futures = new ObjectArrayList<Future<?>>();
            for (int worker = 0; worker < 4; worker++) {
                final int tint = worker;
                futures.add(executor.submit(() -> {
                    final int[] words = BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, tint, 0F, 1F, 0F, 1F);
                    final BakedQuad template = new BakedQuad(words, tint, EnumFacing.SOUTH, null, true, DefaultVertexFormats.ITEM);
                    final BlockGeometryRecord before = pages.capture(words, 7, 0);
                    started.countDown();
                    if (!crossBarrier.await(10, TimeUnit.SECONDS)) throw new AssertionError("Close barrier timed out");
                    for (int i = 0; i < 512; i++) {
                        final BlockGeometryRecord record = i == 0 ? before : pages.capture(words, 7, 0);
                        final var material = pages.material(record.material, template);
                        final var sequence = pages.bind(record.pageShape(), material, null, null);
                        final BakedQuad decoded = sequence.quad(0);
                        assertArrayEquals(words, decoded.getVertexData());
                        assertEquals(tint, decoded.getTintIndex());
                        assertSame(EnumFacing.SOUTH, decoded.getFace());
                        assertSame(DefaultVertexFormats.ITEM, decoded.getFormat());
                        assertTrue(decoded.shouldApplyDiffuseLighting());
                    }
                    return null;
                }));
            }
            try {
                assertTrue(started.await(10, TimeUnit.SECONDS));
                // All workers retain captured records while the indices are closed.
                pages.seal();
            } finally {
                crossBarrier.countDown();
            }
            for (int index = 0; index < futures.size(); index++) futures.get(index).get(20, TimeUnit.SECONDS);
        }
        final var writers = BlockGeometryPages.class.getDeclaredField("writers");
        writers.setAccessible(true);
        for (final Object writer : (Object[]) writers.get(pages)) {
            for (final String name : new String[]{"current", "completed", "captures", "capturedMaterials"}) {
                final var field = writer.getClass().getDeclaredField(name);
                field.setAccessible(true);
                assertNull(field.get(writer), "Late encoders must not restore the closed generation's indices");
            }
        }
        // A later reload gets fresh, open indices rather than the detached writer.
        final BlockGeometryPages next = new BlockGeometryPages(1);
        final int[] words = {1, 2, 3};
        assertSame(next.capture(words, 7, 0), next.capture(words, 7, 0));
        assertNotSame(pages.capture(words, 7, 0), pages.capture(words, 7, 0));
        next.seal();
    }

    private static Object pageValue(final BlockGeometryPages.Sequence sequence, final String name, final int index) throws Exception {
        final var page = sequence.getClass().getDeclaredField("page");
        page.setAccessible(true);
        final Object value = page.get(sequence);
        final var data = value.getClass().getDeclaredField(name);
        data.setAccessible(true);
        return ((Object[]) data.get(value))[index];
    }

    @Test
    void oneAndTwoQuadBindingsKeepTheirIdsWithoutAnArray() throws Exception {
        final BlockGeometryPages pages = new BlockGeometryPages(1);
        final BlockGeometryRecord first = BlockGeometryRecord.capture(
            BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 0F, 0F, 1F, 0F, 1F), 7, 0);
        final BlockGeometryRecord second = BlockGeometryRecord.capture(
            BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 0.5F, 0F, 1F, 0F, 1F), 7, 0);
        final BlockGeometryPages.Shape[] shapes = {first.pageShape(), second.pageShape()};
        for (final EnumFacing facing : new EnumFacing[]{null, EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH,
            EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST}) {
            final BlockGeometryPages.Material[] materials = {
                new BlockGeometryPages.Material(first.material,
                    new BakedQuad(first.decode(), Integer.MIN_VALUE, facing, null, false, DefaultVertexFormats.ITEM)),
                new BlockGeometryPages.Material(second.material,
                    new BakedQuad(second.decode(), Integer.MAX_VALUE, facing, null, true, DefaultVertexFormats.ITEM))
            };
            for (int count = 1; count <= 2; count++) {
                final BlockGeometryPages.Sequence sequence = pages.bind(shapes, materials, 0, count);
                assertEquals(count, sequence.size());
                assertSame(sequence, pages.bind(shapes, materials, 0, count));
                for (final var field : sequence.getClass().getDeclaredFields()) assertFalse(field.getType().isArray());
                for (int index = 0; index < count; index++) {
                    final BakedQuad quad = sequence.quad(index);
                    assertArrayEquals(index == 0 ? first.decode() : second.decode(), quad.vertexData);
                    assertSame(facing, quad.getFace());
                    assertEquals(index == 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE, quad.getTintIndex());
                    assertEquals(index != 0, quad.shouldApplyDiffuseLighting());
                }
            }
        }
        pages.seal();
    }

    @Test
    void packedBindingsRetainTheLastPageIdAndSplitAtRollover() {
        final BlockGeometryPages pages = new BlockGeometryPages(1);
        final BlockGeometryPages.Shape[] shapes = new BlockGeometryPages.Shape[BlockGeometryPages.PAGE_SIZE + 1];
        final BlockGeometryPages.Material[] materials = new BlockGeometryPages.Material[shapes.length];
        for (int index = 0; index < shapes.length; index++) {
            final BlockGeometryRecord record = BlockGeometryRecord.capture(
                BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, index, 0F, 1F, 0F, 1F), 7, 0);
            shapes[index] = record.pageShape();
            materials[index] = new BlockGeometryPages.Material(record.material,
                new BakedQuad(new int[28], index, EnumFacing.SOUTH, null, true, DefaultVertexFormats.ITEM));
        }
        pages.bind(shapes, materials, 0, BlockGeometryPages.PAGE_SIZE - 1);
        final BlockGeometryPages.Sequence edge = pages.bind(shapes, materials, BlockGeometryPages.PAGE_SIZE - 2, 2);
        assertEquals(2, edge.size());
        final BlockGeometryPages.Sequence split = pages.bind(shapes, materials, BlockGeometryPages.PAGE_SIZE - 1, 2);
        pages.seal();
        assertEquals(2, split.size());
        for (int index = 0; index < 2; index++) {
            assertArrayEquals(BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, BlockGeometryPages.PAGE_SIZE - 2 + index,
                0F, 1F, 0F, 1F), edge.quad(index).vertexData);
            assertArrayEquals(BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, BlockGeometryPages.PAGE_SIZE - 1 + index,
                0F, 1F, 0F, 1F), split.quad(index).vertexData);
            assertEquals(BlockGeometryPages.PAGE_SIZE - 2 + index, edge.quad(index).getTintIndex());
            assertEquals(BlockGeometryPages.PAGE_SIZE - 1 + index, split.quad(index).getTintIndex());
        }
    }

    @Test
    void generationBarrierSharesShapesMaterialsAndColumnsAcrossSealedPages() throws Exception {
        final BlockGeometryPages pages = new BlockGeometryPages(1);
        final BlockGeometryPages.Shape[] shapes = new BlockGeometryPages.Shape[BlockGeometryPages.PAGE_SIZE];
        final BlockGeometryPages.Material[] materials = new BlockGeometryPages.Material[shapes.length];
        final BakedQuad template = new BakedQuad(new int[28], -1, EnumFacing.SOUTH, null, true, DefaultVertexFormats.ITEM);
        for (int i = 0; i < shapes.length; i++) {
            final BlockGeometryRecord r = BlockGeometryRecord.capture(BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, i, 0F, 1F, 0F, 1F), 7, 0);
            shapes[i] = new BlockGeometryPages.Shape(r.shape, r.stride, r.position);
            materials[i] = new BlockGeometryPages.Material(r.material, template);
        }
        final BlockGeometryPages.Sequence first = pages.bind(shapes, materials, 0, shapes.length);
        final BlockGeometryRecord extra = BlockGeometryRecord.capture(BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 5000, 0F, 1F, 0F, 1F), 7, 0);
        pages.bind(new BlockGeometryPages.Shape[]{new BlockGeometryPages.Shape(extra.shape, 7, 0)},
            new BlockGeometryPages.Material[]{new BlockGeometryPages.Material(extra.material, template)}, 0, 1);
        final BlockGeometryRecord duplicate = BlockGeometryRecord.capture(BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 0, 0F, 1F, 0F, 1F), 7, 0);
        final BlockGeometryPages.Sequence last = pages.bind(new BlockGeometryPages.Shape[]{new BlockGeometryPages.Shape(duplicate.shape, 7, 0)},
            new BlockGeometryPages.Material[]{new BlockGeometryPages.Material(duplicate.material, template)}, 0, 1);
        final BakedQuad otherTint = new BakedQuad(new int[28], 7, EnumFacing.SOUTH, null, true, DefaultVertexFormats.ITEM);
        pages.bind(new BlockGeometryPages.Shape[]{new BlockGeometryPages.Shape(duplicate.shape, 7, 0)},
            new BlockGeometryPages.Material[]{new BlockGeometryPages.Material(duplicate.material, otherTint)}, 0, 1);
        assertNotSame(pageValue(first, "shapes", 0), pageValue(last, "shapes", 1));
        pages.seal();
        assertSame(pageValue(first, "shapes", 0), pageValue(last, "shapes", 1));
        assertSame(pageValue(first, "materials", 0), pageValue(last, "materials", 0));
        final var columns = BlockGeometryPages.Material.class.getDeclaredField("columns");
        columns.setAccessible(true);
        assertSame(columns.get(pageValue(last, "materials", 0)), columns.get(pageValue(last, "materials", 1)));
        assertArrayEquals(BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 0, 0F, 1F, 0F, 1F), last.quad(0).vertexData);
        // The generation no longer retains the pages or the capture index after publishing.
        final var writers = BlockGeometryPages.class.getDeclaredField("writers");
        writers.setAccessible(true);
        for (final Object writer : (Object[]) writers.get(pages)) {
            for (final String name : new String[]{"current", "completed", "captures", "capturedMaterials"}) {
                final var field = writer.getClass().getDeclaredField(name);
                field.setAccessible(true);
                assertNull(field.get(writer));
            }
        }
    }

    @Test
    void boundedCaptureChecksEveryWordAndReleasesItsIndexAtClose() {
        final BlockGeometryPages pages = new BlockGeometryPages(1);
        final int[] words = BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 0, 0F, 1F, 0F, 1F);
        final BlockGeometryRecord first = pages.capture(words, 7, 0);
        assertSame(first, pages.capture(words.clone(), 7, 0));
        assertSame(first.pageShape(), pages.capture(words.clone(), 7, 0).pageShape());
        final BakedQuad template = new BakedQuad(words, -1, EnumFacing.SOUTH, null, true, DefaultVertexFormats.ITEM);
        assertSame(pages.material(first.material, template), pages.material(first.material, template));
        assertNotSame(pages.material(first.material, template), pages.material(first.material,
            new BakedQuad(words, 5, EnumFacing.SOUTH, null, true, DefaultVertexFormats.ITEM)));
        final int[] original = words.clone();
        words[4] ^= 1;
        assertNotSame(first, pages.capture(words, 7, 0));
        assertArrayEquals(original, first.decode());
        // Arrays.hashCode collisions must never make two raw geometries alias.
        final int[] a = {0, 31}, b = {1, 0};
        assertEquals(Arrays.hashCode(a), Arrays.hashCode(b));
        assertArrayEquals(a, pages.capture(a, 7, 0).decode());
        assertArrayEquals(b, pages.capture(b, 7, 0).decode());
        pages.seal();
        assertNotSame(first, pages.capture(original, 7, 0));
    }

    @Test
    void sequenceHashCollisionsKeepDifferentGeometryAndAnExactHitReusesTheBinding() {
        final BlockGeometryPages pages = new BlockGeometryPages(1);
        final BlockGeometryRecord first = BlockGeometryRecord.capture(
            BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 0F, 0F, 1F, 0F, 1F), 7, 0);
        final BlockGeometryRecord second = BlockGeometryRecord.capture(
            BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 0.5F, 0F, 1F, 0F, 1F), 7, 0);
        final BlockGeometryPages.Shape[] shapes = {first.pageShape(), second.pageShape()};
        final BlockGeometryPages.Material[] materials = new BlockGeometryPages.Material[32];
        for (int tint = 0; tint < materials.length; tint++) {
            materials[tint] = new BlockGeometryPages.Material(first.material,
                new BakedQuad(first.decode(), tint, EnumFacing.SOUTH, null, true, DefaultVertexFormats.ITEM));
            pages.bind(shapes, new BlockGeometryPages.Material[]{materials[tint]}, 0, 1);
        }
        // The page-local pairs [shape 0, material 31] and [shape 1, material 0] collide.
        assertEquals(Arrays.hashCode(new short[]{0, 31}), Arrays.hashCode(new short[]{1, 0}));
        final BlockGeometryPages.Sequence a = pages.bind(new BlockGeometryPages.Shape[]{shapes[0]},
            new BlockGeometryPages.Material[]{materials[31]}, 0, 1);
        final BlockGeometryPages.Sequence b = pages.bind(new BlockGeometryPages.Shape[]{shapes[1]},
            new BlockGeometryPages.Material[]{materials[0]}, 0, 1);
        assertNotSame(a, b);
        assertEquals(31, a.quad(0).getTintIndex());
        assertEquals(0, b.quad(0).getTintIndex());
        assertArrayEquals(first.decode(), a.quad(0).vertexData);
        assertArrayEquals(second.decode(), b.quad(0).vertexData);
        assertSame(b, pages.bind(new BlockGeometryPages.Shape[]{shapes[1]},
            new BlockGeometryPages.Material[]{materials[0]}, 0, 1));
        pages.seal();
        assertArrayEquals(first.decode(), a.quad(0).vertexData);
        assertArrayEquals(second.decode(), b.quad(0).vertexData);
    }

    @Test
    void pageRolloverPreservesOrderBeyond65535Shapes() throws Exception {
        final int count = 65538;
        final BlockGeometryRecord[] geometry = new BlockGeometryRecord[count];
        final BlockGeometryPages.Shape[] shapes = new BlockGeometryPages.Shape[count];
        final BlockGeometryPages.Material[] materials = new BlockGeometryPages.Material[count];
        final BakedQuad template = new BakedQuad(new int[28], -1, EnumFacing.SOUTH, null, true, DefaultVertexFormats.ITEM);
        for (int index = 0; index < count; index++) {
            geometry[index] = BlockGeometryRecord.capture(BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, index, 0F, 1F, 0F, 1F), 7, 0);
            shapes[index] = new BlockGeometryPages.Shape(geometry[index].shape, geometry[index].stride, geometry[index].position);
            materials[index] = new BlockGeometryPages.Material(geometry[index].material, template);
        }
        final BlockGeometryPages pages = new BlockGeometryPages();
        final BlockGeometryPages.Sequence sequence = pages.bind(shapes, materials, 0, count);
        final var writerField = BlockGeometryPages.class.getDeclaredField("writers");
        writerField.setAccessible(true);
        for (final Object writer : (Object[]) writerField.get(pages)) {
            final var currentField = writer.getClass().getDeclaredField("current");
            currentField.setAccessible(true);
            final Object page = currentField.get(writer);
            if (page == null) continue;
            for (final String name : new String[]{"shapeIndex", "materialIndex", "sequences"}) {
                final var field = page.getClass().getDeclaredField(name);
                field.setAccessible(true);
                assertTrue(((Map<?, ?>) field.get(page)).size() <= BlockGeometryPages.PAGE_SIZE);
            }
        }
        pages.seal();
        assertEquals(count, sequence.size());
        for (final int index : new int[]{0, 32767, 32768, 65535, 65536, 65537}) {
            assertArrayEquals(geometry[index].decode(), sequence.quad(index).vertexData);
        }
    }

    @Test
    void anEncoderThatAlreadyHeldTheAttachmentCanFinishAfterGenerationClose() throws Exception {
        final BlockGeometryPages pages = new BlockGeometryPages(1);
        pages.seal();
        final BlockGeometryRecord record = BlockGeometryRecord.capture(BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 1F, 0F, 1F, 0F, 1F), 7, 0);
        final BakedQuad template = new BakedQuad(record.decode(), -1, EnumFacing.SOUTH, null, true, DefaultVertexFormats.ITEM);
        final BlockGeometryPages.Sequence result = pages.bind(new BlockGeometryPages.Shape[]{new BlockGeometryPages.Shape(record.shape, record.stride, record.position)},
            new BlockGeometryPages.Material[]{new BlockGeometryPages.Material(record.material, template)}, 0, 1);
        assertArrayEquals(record.decode(), result.quad(0).vertexData);
        final var pageField = result.getClass().getDeclaredField("page");
        pageField.setAccessible(true);
        final Object page = pageField.get(result);
        for (final String name : new String[]{"shapeIndex", "materialIndex", "sequences"}) {
            final var field = page.getClass().getDeclaredField(name);
            field.setAccessible(true);
            assertNull(field.get(page), "A detached writer must not leave a new index alive");
        }
    }

}
