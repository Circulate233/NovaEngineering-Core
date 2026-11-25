package github.kasuminova.novaeng.client.memory;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ScopedAnimationEditTest {
    @Test
    void preservesSharedPixelsAndEditsSurviveNestedPacking() {
        OwnedAnimationFrames frames = new OwnedAnimationFrames();
        int[] level = new int[4096];
        int[] high = new int[1024];
        frames.internalView().add(new int[][]{level, high});
        frames.internalView().add(new int[][]{level, high});
        frames.internalView().add(new int[][]{new int[4096], high});
        frames.pack();
        assertEquals(3, frames.editBaseLevels(0, (index, pixels) -> {
            frames.pack();
            pixels[0] += 1;
            return index + 1;
        }).intValue());
        assertEquals(2, frames.internalView().get(0)[0][0]);
        assertEquals(2, frames.internalView().get(1)[0][0]);
        assertEquals(1, frames.internalView().get(2)[0][0]);
        assertEquals(0, frames.internalView().get(2)[1][0]);
    }

    @Test
    void keepsPublicAliasesAndReadsReplacementBaseFromPublishedOuter() {
        OwnedAnimationFrames frames = new OwnedAnimationFrames();
        int[] shared = {20};
        int[][] exposed = {shared, new int[]{99}};
        frames.add(exposed);
        int[][] privateFrame = {shared};
        frames.internalView().add(privateFrame);
        frames.internalView().add(new int[0][]);
        frames.internalView().add(null);
        frames.internalView().add(new int[][]{null});
        int[] replacement = {30};
        exposed[0] = replacement;
        assertEquals(2, frames.editBaseLevels(0, (n, pixels) -> { pixels[0]++; return n + 1; }).intValue());
        assertSame(exposed, frames.get(0));
        assertSame(replacement, frames.get(0)[0]);
        assertArrayEquals(new int[]{31}, replacement);
        assertArrayEquals(new int[]{21}, shared);
        assertArrayEquals(new int[]{99}, exposed[1]);
    }

    @Test
    void releasesDeferredPackWhenEditorThrowsAndKeepsDataPrivate() throws Exception {
        OwnedAnimationFrames frames = new OwnedAnimationFrames();
        for (int n=0; n<4; n++) frames.internalView().add(new int[][]{new int[4096]});
        assertThrows(IllegalArgumentException.class, () -> frames.editBaseLevels(0, (state, data) -> {
            data[11] = 381;
            frames.pack();
            throw new IllegalArgumentException("test");
        }));
        assertEquals(381, frames.internalView().get(0)[0][11]);
        var listField = OwnedAnimationFrames.class.getDeclaredField("frames");
        listField.setAccessible(true);
        for (Object frame : (List<?>) listField.get(frames)) {
            var flag = frame.getClass().getDeclaredField("published");
            var source = frame.getClass().getDeclaredField("source");
            flag.setAccessible(true);
            source.setAccessible(true);
            assertFalse(flag.getBoolean(frame));
            assertNull(source.get(frame));
        }
    }
}
