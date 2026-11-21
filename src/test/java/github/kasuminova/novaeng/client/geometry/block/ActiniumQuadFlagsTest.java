package github.kasuminova.novaeng.client.geometry.block;

import com.dhj.actinium.mixin.vintage.core.terrain.MixinBakedQuad;
import dhj.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ActiniumQuadFlagsTest {
    @Test
    void publicQuadViewPreservesProducerFlagsAndComputedMarker() {
        final BakedQuadView original = new ActiniumQuad();
        original.addFlags(8 | 0x4000);
        final int captured = original.getFlags();
        assertEquals(8 | 0x4000, captured & (8 | 0x4000));
        assertNotEquals(0, captured & Integer.MIN_VALUE);
        final BakedQuadView restored = new ActiniumQuad();
        restored.addFlags(captured);
        assertEquals(captured, restored.getFlags());
        assertEquals(captured, restored.getFlags());
    }

    /**
     * Runs the actual dependency's flags implementation without starting a transformed game.
     */
    public static class ActiniumQuad extends MixinBakedQuad {
        private final int[] words = new int[28];

        public ActiniumQuad() {
            face = EnumFacing.SOUTH;
            applyDiffuseLighting = true;
            final int[] corners = {0, 1, 3, 2};
            for (int vertex = 0; vertex < 4; vertex++) {
                words[vertex * 7] = Float.floatToRawIntBits(corners[vertex] & 1);
                words[vertex * 7 + 1] = Float.floatToRawIntBits((corners[vertex] >>> 1) & 1);
                words[vertex * 7 + 2] = Float.floatToRawIntBits(1F);
                words[vertex * 7 + 3] = -1;
            }
        }

        @Override
        public int[] getVertexData() {
            return words;
        }

        @Override
        public VertexFormat getFormat() {
            return DefaultVertexFormats.ITEM;
        }
    }
}
