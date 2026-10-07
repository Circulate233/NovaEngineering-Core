package github.kasuminova.novaeng.common.performance;

/**
 * The existing 1.12/REID Blocks/Data/Add/Add2 layout; no new on-disk format.
 */
public final class SectionIdCodec {
    public static final int SIZE = 4096;

    private SectionIdCodec() {
    }

    public static int bitsFor(final int entries) {
        return Math.max(4, 32 - Integer.numberOfLeadingZeros(Math.max(1, entries) - 1));
    }

    public static void decode(final byte[] blocks, final byte[] data, final byte[] add,
                              final byte[] add2, final int[] palette, final int[] target) {
        if (blocks.length != SIZE || data.length != SIZE / 2 || target.length < SIZE
            || add != null && add.length != SIZE / 2 || add2 != null && add2.length != SIZE / 2) {
            throw new IllegalArgumentException("Invalid chunk section array length");
        }
        for (int i = 0; i < SIZE; i++) {
            final int low = (blocks[i] & 255) << 4 | nibble(data, i);
            if (palette != null) {
                if (low >= palette.length) {
                    throw new IllegalArgumentException("Section palette index " + low + " exceeds " + palette.length);
                }
                target[i] = palette[low];
            } else {
                final int high = (add == null ? 0 : nibble(add, i))
                    | (add2 == null ? 0 : nibble(add2, i) << 4);
                target[i] = high << 12 | low;
            }
        }
    }

    public static void writeIndex(final int index, final int paletteId, final byte[] blocks, final byte[] data) {
        blocks[index] = (byte) (paletteId >>> 4);
        final int offset = index >>> 1;
        final int shift = (index & 1) << 2;
        data[offset] = (byte) ((data[offset] & ~(15 << shift)) | ((paletteId & 15) << shift));
    }

    private static int nibble(final byte[] data, final int index) {
        return data[index >>> 1] >>> ((index & 1) << 2) & 15;
    }
}
