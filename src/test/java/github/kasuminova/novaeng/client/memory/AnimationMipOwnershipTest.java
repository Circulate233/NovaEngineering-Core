package github.kasuminova.novaeng.client.memory;

import org.junit.jupiter.api.Test;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnimationMipOwnershipTest {
    private static void verifyUploads(final OwnedAnimationFrames frames, final int[][][] expected) {
        final List<int[][]> reads = frames.internalView();
        final IntBuffer upload = IntBuffer.allocate(64 * 64);
        for (int frame = 0; frame < expected.length; frame++) {
            final int[][] levels = reads.get(frame);
            for (int level = 0; level < levels.length; level++) {
                final int[] pixels = levels[level];
                assertDoesNotThrow(() -> upload.clear().put(pixels));
                assertArrayEquals(expected[frame][level], pixels);
            }
        }
    }

    private static int[] pixels(final int length, final int value) {
        final int[] pixels = new int[length];
        Arrays.fill(pixels, 0xff000000 | value);
        return pixels;
    }

    @Test
    void packingKeepsMipLevelsFilledThroughThePrivateOuterArray() {
        final OwnedAnimationFrames frames = new OwnedAnimationFrames();
        final int[][][] expected = new int[3][][];
        for (int frame = 0; frame < expected.length; frame++) {
            final int[][] levels = {pixels(64 * 64, frame), null};
            frames.internalView().add(levels);
            levels[1] = pixels(32 * 32, frame + 10);
            expected[frame] = levels.clone();
        }
        frames.pack();
        verifyUploads(frames, expected);
    }

    @Test
    void reusingTheOuterArrayForGeneratedMipmapsKeepsTheNewLevels() {
        final OwnedAnimationFrames before = new OwnedAnimationFrames();
        for (int frame = 0; frame < 3; frame++) {
            before.internalView().add(new int[][]{pixels(64 * 64, frame), null});
        }
        before.prepareMipmaps();
        final int[][][] expected = new int[before.size()][][];
        for (int frame = 0; frame < expected.length; frame++) {
            final int[][] levels = before.internalView().get(frame);
            levels[1] = pixels(32 * 32, frame + 10);
            expected[frame] = levels.clone();
        }
        final OwnedAnimationFrames after = before.mipmaps(before.internalView());
        after.pack();
        verifyUploads(after, expected);
        final int[][] published = before.get(1);
        final int[][] generated = after.get(1);
        assertNotNull(published);
        assertNotNull(generated);
        assertSame(published[1], generated[1]);
    }
}
