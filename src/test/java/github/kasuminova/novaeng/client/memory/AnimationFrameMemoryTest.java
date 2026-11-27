package github.kasuminova.novaeng.client.memory;

import github.kasuminova.novaeng.client.geometry.item.ItemContour;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AnimationFrameMemoryTest {
    @Test
    void itemContoursConsumeBorrowedFramesWithoutPublishingTheirPixelArrays() throws Exception {
        final OwnedAnimationFrames frames = new OwnedAnimationFrames();
        final int[][] expected = new int[8][];
        for (int frame = 0; frame < expected.length; frame++) {
            final int[] pixels = new int[128 * 128];
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    pixels[y * 128 + x] = x < 12 + frame * 13 && y < 120 - frame * 11 ? 0xff123456 : 0;
                }
            }
            expected[frame] = pixels;
            frames.internalView().add(new int[][]{pixels});
        }
        frames.pack();
        assertEquals(ItemContour.create(128, 128, expected), ItemContour.createFromMipmaps(128, 128, frames.internalView()));
        assertEquals(ItemContour.create(128, 128, Arrays.copyOf(expected, 3)),
            ItemContour.createFromMipmaps(128, 128, 3, frames.internalView()));
        final var entries = OwnedAnimationFrames.class.getDeclaredField("frames");
        entries.setAccessible(true);
        for (final Object frame : (List<?>) entries.get(frames)) {
            final var published = frame.getClass().getDeclaredField("published");
            published.setAccessible(true);
            assertFalse(published.getBoolean(frame));
            final var source = frame.getClass().getDeclaredField("source");
            source.setAccessible(true);
            assertNull(source.get(frame));
        }
        final int[][] exposed = frames.getFirst();
        assertSame(exposed, frames.getFirst());
        exposed[0][0] = 0x12345678;
        assertEquals(0x12345678, frames.internalView().getFirst()[0][0]);
    }

    @Test
    void packingIncludesModestAnimationSequencesAndReusesTheAtlasEncoder() throws Exception {
        final OwnedAnimationFrames frames = new OwnedAnimationFrames();
        for (int frame = 0; frame < 8; frame++) {
            final int[] pixels = new int[32 * 32];
            Arrays.fill(pixels, 0xff000000 | frame);
            frames.internalView().add(new int[][]{pixels});
        }
        frames.pack(new PixelFrameCodec.Encoder());
        final var entries = OwnedAnimationFrames.class.getDeclaredField("frames");
        entries.setAccessible(true);
        final List<?> packedFrames = (List<?>) entries.get(frames);
        for (int index = 0; index < packedFrames.size(); index++) {
            final Object frame = packedFrames.get(index);
            final var source = frame.getClass().getDeclaredField("source");
            source.setAccessible(true);
            assertNull(source.get(frame), "32 KiB animation should retain encoded frames");
        }
        for (int frame = 0; frame < frames.size(); frame++) {
            final int[] pixels = frames.internalView().get(frame)[0];
            for (int index = 0; index < pixels.length; index++) assertEquals(0xff000000 | frame, pixels[index]);
        }
    }

    @Test
    void codecRoundTripsArbitraryBitsRunsAndUnalignedMatches() {
        final Random random = new Random(178139);
        final int[] dictionary = new int[16384];
        final PixelFrameCodec.Encoder encoder = new PixelFrameCodec.Encoder();
        for (int trial = 0; trial < 500; trial++) {
            final int length = trial < 80 ? trial : random.nextInt(12000);
            final int[] input = new int[length];
            for (int i = 0; i < length; i++) input[i] = switch (trial % 5) {
                case 0 -> random.nextInt();
                case 1 -> i % 9;
                case 2 -> 0xff336699;
                case 3 -> random.nextInt(5) * 0x1020304;
                default -> 0xff000000 | (i / 13 & 255);
            };
            final byte[] encoded = PixelFrameCodec.encode(input, dictionary);
            final int[] output = new int[length];
            Arrays.fill(output, -1);
            PixelFrameCodec.decode(encoded, output);
            assertArrayEquals(input, output, "trial " + trial);
            final byte[] useful = encoder.encodeIfSmaller(input);
            if (encoded.length + 32L < input.length * 4L) assertArrayEquals(encoded, useful);
            else assertNull(useful);
        }
        assertThrows(IllegalArgumentException.class, () -> PixelFrameCodec.decode(new byte[]{0, 0, 0}, new int[3]));
    }

    @Test
    void bulkDecoderPreservesOverlappingRunsAtEveryByteAlignment() {
        final int[] dictionary = new int[16384];
        for (int distance = 1; distance <= 32; distance++) {
            for (int prefix = 0; prefix < 4; prefix++) {
                final int[] input = new int[4097];
                for (int octet = 0; octet < input.length * 4; octet++) {
                    final int value = octet < prefix ? 251 - octet : ((octet - prefix) % distance) * 7 + 3;
                    input[octet >>> 2] |= (value & 255) << ((octet & 3) * 8);
                }
                final int[] actual = new int[input.length];
                PixelFrameCodec.decode(PixelFrameCodec.encode(input, dictionary), actual);
                assertArrayEquals(input, actual, "distance=" + distance + ", prefix=" + prefix);
            }
        }
    }

    @Test
    void reusedEncoderDoesNotAllocateDiscardedIncompressiblePayloads() {
        final PixelFrameCodec.Encoder encoder = new PixelFrameCodec.Encoder();
        final int[] input = new Random(41).ints(1024).toArray();
        for (int i = 0; i < 256; i++) assertNull(encoder.encodeIfSmaller(input));
        final var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        final long thread = Thread.currentThread().threadId();
        final long start = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 100; i++) encoder.encodeIfSmaller(input);
        assertTrue(bean.getThreadAllocatedBytes(thread) - start < 2048);
    }

    @Test
    void incompressibleLiteralBlockDoesNotGrowTheDiscardedOutputBuffer() {
        final int[] input = new Random(41).ints(4096).toArray();
        final PixelFrameCodec.Encoder encoder = new PixelFrameCodec.Encoder();
        final var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        final long thread = Thread.currentThread().threadId();
        final long start = bean.getThreadAllocatedBytes(thread);
        final byte[] result = encoder.encodeIfSmaller(input);
        final long allocated = bean.getThreadAllocatedBytes(thread) - start;
        assertNull(result);
        assertTrue(allocated < 4096, "discarded output allocation=" + allocated);
        final int[] compressible = new int[input.length];
        Arrays.fill(compressible, 0xff123456);
        final byte[] payload = encoder.encodeIfSmaller(compressible);
        assertNotNull(payload);
        final int[] restored = new int[compressible.length];
        PixelFrameCodec.decode(payload, restored);
        assertArrayEquals(compressible, restored);
    }

    private static int[][] frame(final int number) {
        final int[][] frame = new int[][]{new int[16384], new int[4096], null};
        for (int level = 0; level < 2; level++) {
            for (int i = 0; i < frame[level].length; i++) frame[level][i] = 0xff000000 | number << 16 | i % 17;
        }
        return frame;
    }

    @Test
    void privateAnimationUsesTwoReusableWindowsWithoutPerFrameAllocation() {
        final OwnedAnimationFrames frames = new OwnedAnimationFrames();
        final List<int[][]> privateView = frames.internalView();
        for (int i = 0; i < 8; i++) privateView.add(frame(i));
        frames.pack();
        for (int i = 0; i < 8; i++) {
            final int[][] decoded = privateView.get(i);
            final int[][] expected = frame(i);
            assertArrayEquals(expected[0], decoded[0]);
            assertArrayEquals(expected[1], decoded[1]);
            assertNull(decoded[2]);
        }
        final int[][] first = privateView.get(0);
        privateView.get(1);
        assertSame(first, privateView.get(2));
        for (int i = 0; i < 100; i++) privateView.get(i % 8);
        final com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        final long before = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
        int checksum = 0;
        for (int i = 0; i < 2000; i++) checksum ^= privateView.get(i % 8)[0][i % 16384];
        final long allocation = bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - before;
        assertTrue(allocation < 4096, "allocation=" + allocation + ", checksum=" + checksum);
    }

    @Test
    void publicReadsPreserveBothIndependentFramesAndOriginalPixelAliases() {
        final OwnedAnimationFrames frames = new OwnedAnimationFrames();
        final int[][] original = frame(1);
        frames.internalView().add(original);
        frames.internalView().add(new int[][]{original[0], original[1], null});
        frames.internalView().add(frame(1));
        frames.internalView().add(frame(2));
        frames.pack();
        final int[][] exposed = frames.get(0);
        assertSame(exposed, frames.get(0));
        assertSame(exposed[0], frames.get(1)[0]);
        assertNotSame(exposed[0], frames.get(2)[0]);
        exposed[0][13] = 0x12345678;
        assertEquals(0x12345678, frames.internalView().get(1)[0][13]);
        exposed[1] = new int[]{5, 6};
        assertSame(exposed, frames.internalView().get(0));
        assertArrayEquals(new int[]{5, 6}, frames.get(0)[1]);
    }

    @Test
    void externallyInsertedArraysAndOldMipmapListsKeepTheirAliases() {
        final OwnedAnimationFrames original = new OwnedAnimationFrames();
        for (int i = 0; i < 5; i++) original.internalView().add(frame(i));
        original.prepareMipmaps();
        final List<int[][]> generated = new ObjectArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            final int[][] prior = original.internalView().get(i);
            generated.add(new int[][]{prior[0], prior[1], new int[1024]});
        }
        final OwnedAnimationFrames next = original.mipmaps(generated);
        next.pack();
        final int[][] oldPublic = original.get(2);
        oldPublic[0][19] = 78654;
        assertEquals(78654, next.internalView().get(2)[0][19]);
        assertSame(oldPublic[0], next.get(2)[0]);
        final int[][] external = frame(7);
        next.add(external);
        next.pack();
        assertSame(external, next.getLast());
        external[0][1] = 34634;
        assertEquals(34634, next.internalView().getLast()[0][1]);
        next.clear();
        assertTrue(next.isEmpty());
    }

    @Test
    void clearingAnOldMipmapListKeepsTheSurvivingGenerationsPixelAliases() {
        final OwnedAnimationFrames original = new OwnedAnimationFrames();
        for (int i = 0; i < 5; i++) original.internalView().add(frame(i));
        original.prepareMipmaps();
        final List<int[][]> generated = new ObjectArrayList<>();
        for (int i = 0; i < original.size(); i++) generated.add(original.internalView().get(i).clone());
        final OwnedAnimationFrames next = original.mipmaps(generated);
        original.clear();
        next.internalView().add(new int[][]{next.internalView().getFirst()[0], null, null});
        next.pack();
        final int[] published = next.getFirst()[0];
        published[13] = 0x12345678;
        assertSame(published, next.getLast()[0]);
        assertEquals(0x12345678, next.internalView().getLast()[0][13]);
    }

    @Test
    void insertingAfterPackingRecoversPublishedPixelAndOuterArrayAliases() {
        final OwnedAnimationFrames frames = new OwnedAnimationFrames();
        for (int i = 0; i < 5; i++) frames.internalView().add(frame(i));
        frames.pack();
        final int[][] exposed = frames.getFirst();
        // Replacing a public mip level must also be visible to newly inserted aliases.
        exposed[1] = frame(17)[1];
        frames.internalView().add(new int[][]{exposed[0], exposed[1], null});
        frames.internalView().add(exposed);
        frames.pack();
        exposed[0][19] = 0x12345678;
        exposed[1][13] = 0x76543210;
        assertSame(exposed, frames.getLast());
        assertSame(exposed[0], frames.get(frames.size() - 2)[0]);
        assertSame(exposed[1], frames.get(frames.size() - 2)[1]);
        assertEquals(0x12345678, frames.internalView().get(frames.size() - 2)[0][19]);
        assertEquals(0x76543210, frames.internalView().getLast()[1][13]);
    }

    @Test
    void packingOldGenerationDoesNotCompressAFormerListThatHasBeenClearedAndReused() throws Exception {
        final OwnedAnimationFrames original = new OwnedAnimationFrames();
        for (int i = 0; i < 5; i++) original.internalView().add(frame(i));
        original.prepareMipmaps();
        final List<int[][]> generated = new ObjectArrayList<>();
        for (int i = 0; i < original.size(); i++) generated.add(original.internalView().get(i).clone());
        final OwnedAnimationFrames next = original.mipmaps(generated);
        original.clear();
        final int[][] reused = frame(19);
        original.internalView().add(reused);
        next.pack();
        // A frame in another ownership group must not acquire a compressed payload
        // while its own source index still keeps the full raw arrays alive.
        assertSame(reused, original.internalView().getFirst());
        final var framesField = OwnedAnimationFrames.class.getDeclaredField("frames");
        framesField.setAccessible(true);
        final Object entry = ((List<?>) framesField.get(original)).getFirst();
        final var sourceField = entry.getClass().getDeclaredField("source");
        sourceField.setAccessible(true);
        assertSame(reused, sourceField.get(entry));
        assertArrayEquals(frame(19)[0], original.getFirst()[0]);
    }

    @Test
    void publicLevelReplacementReleasesObsoleteMipStorageAndPreservesOtherAliases() throws Exception {
        final OwnedAnimationFrames frames = new OwnedAnimationFrames();
        final int[][] original = frame(1);
        original[0] = Arrays.copyOf(original[0], 65536);
        final int[] shared = original[0];
        frames.internalView().add(original);
        frames.internalView().add(new int[][]{shared, null, null});
        frames.pack();
        frames.internalView().getFirst();
        final int[][] exposed = frames.getFirst();
        final var firstWindow = OwnedAnimationFrames.class.getDeclaredField("first");
        firstWindow.setAccessible(true);
        assertNull(firstWindow.get(frames), "Publishing must release this frame's obsolete decode window");
        final int[] previousShared = exposed[0];
        final int[] replacement = frame(8)[0];
        exposed[0] = replacement;
        exposed[1] = null;
        assertSame(exposed, frames.getFirst());
        final var framesField = OwnedAnimationFrames.class.getDeclaredField("frames");
        framesField.setAccessible(true);
        final Object entry = ((List<?>) framesField.get(frames)).getFirst();
        final var mipsField = entry.getClass().getDeclaredField("mips");
        mipsField.setAccessible(true);
        final Object[] mips = (Object[]) mipsField.get(entry);
        final var rawField = mips[0].getClass().getDeclaredField("raw");
        rawField.setAccessible(true);
        assertSame(replacement, rawField.get(mips[0]), "The frame must release its replaced raw mip");
        assertNull(mips[1], "A removed public mip must not retain its previous pixel array");
        assertSame(previousShared, frames.getLast()[0], "A different frame still owns the original alias");
        previousShared[19] = 0x12345678;
        assertEquals(0x12345678, frames.internalView().getLast()[0][19]);
        assertNotSame(previousShared, frames.getFirst()[0]);
    }

    @Test
    void repackingRecognizesAliasesAssignedToPublishedOuterArraysWithoutAnInsertion() {
        final OwnedAnimationFrames frames = new OwnedAnimationFrames();
        final int[][] first = frame(1);
        final int[] privatePixels = new Random(71).ints(65536).toArray();
        frames.internalView().add(first);
        frames.internalView().add(new int[][]{privatePixels});
        frames.getFirst();
        frames.pack();
        final int[][] published = frames.getFirst();
        published[0] = privatePixels;
        assertSame(published, frames.getFirst());
        Arrays.fill(privatePixels, 0xff123456);
        // No frame()/prepareMipmaps() call rebuilds the lookup tables before this repack.
        frames.pack();
        assertSame(privatePixels, frames.getLast()[0]);
        assertSame(privatePixels, frames.getFirst()[0]);
        privatePixels[13] = 0x76543210;
        assertEquals(0x76543210, frames.internalView().getLast()[0][13]);
    }
}
