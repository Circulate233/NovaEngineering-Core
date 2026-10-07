package github.kasuminova.novaeng.common.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SectionIdCodecTest {
    @Test
    void selectsTheSmallestLocalWidthAtEveryBoundary() {
        final int[] sizes = {16, 17, 32, 33, 64, 65, 128, 129, 256, 257};
        final int[] bits = {4, 5, 5, 6, 6, 7, 7, 8, 8, 9};
        for (int i = 0; i < sizes.length; i++) {
            assertEquals(bits[i], SectionIdCodec.bitsFor(sizes[i]));
        }
    }

    @Test
    void reidPaletteRoundTripPreservesFullIdsAndOwnsItsOutput() {
        final SectionPalette<Object> palette = new SectionPalette<>();
        final Object[] states = new Object[257];
        final int[] global = new int[states.length];
        for (int i = 0; i < states.length; i++) {
            states[i] = new Object();
            global[i] = 0x120000 + i;
        }
        final byte[] blocks = new byte[4096];
        final byte[] data = new byte[2048];
        for (int i = 0; i < 4096; i++) {
            final int state = i % states.length;
            SectionIdCodec.writeIndex(i, palette.indexFor(states[state], global[state]), blocks, data);
        }
        final int[] owned = palette.snapshot();
        palette.clear();
        palette.indexFor(new Object(), 42);
        final int[] decoded = new int[4096];
        SectionIdCodec.decode(blocks, data, null, null, owned, decoded);
        for (int i = 0; i < 4096; i++) {
            assertEquals(global[i % states.length], decoded[i]);
        }
        assertEquals(257, owned.length);
    }

    @Test
    void legacyAddAndAdd2KeepBothHighNibbles() {
        final byte[] blocks = new byte[4096];
        final byte[] data = new byte[2048];
        final byte[] add = new byte[2048];
        final byte[] add2 = new byte[2048];
        blocks[0] = (byte) 0xbc;
        data[0] = (byte) 0xad;
        add[0] = 0x21;
        add2[0] = 0x43;
        final int[] decoded = new int[4096];
        SectionIdCodec.decode(blocks, data, add, add2, null, decoded);
        assertEquals(0x31bcd, decoded[0]);
        assertEquals(0x4200a, decoded[1]);
        SectionIdCodec.decode(blocks, data, null, null, null, decoded);
        assertEquals(0xbcd, decoded[0]);
        assertEquals(0xa, decoded[1]);
    }

    @Test
    void corruptPaletteCannotReadPastItsOwner() {
        final byte[] blocks = new byte[4096];
        blocks[0] = 1;
        assertThrows(IllegalArgumentException.class, () ->
            SectionIdCodec.decode(blocks, new byte[2048], null, null, new int[]{0}, new int[4096]));
    }
}
