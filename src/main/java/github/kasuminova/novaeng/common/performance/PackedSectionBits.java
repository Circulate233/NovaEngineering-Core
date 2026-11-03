package github.kasuminova.novaeng.common.performance;

/**
 * Copies local palette IDs into a fresh, wider standard Minecraft packed array.
 */
public final class PackedSectionBits {
    private PackedSectionBits() {
    }

    public static void pack(final short[] source, final int sourceOffset, final int[] translatedIds,
                            final long[] destination, final int bits, final int size) {
        final long mask = (1L << bits) - 1;
        int word = 0;
        int offset = 0;
        long packed = 0;
        for (int i = 0; i < size; i++) {
            final long id = translatedIds[source[sourceOffset + i] & 0xffff] & mask;
            packed |= id << offset;
            final int end = offset + bits;
            if (end >= 64) {
                destination[word++] = packed;
                packed = end == 64 ? 0 : id >>> (64 - offset);
                offset = end - 64;
            } else {
                offset = end;
            }
        }
        if (offset != 0) destination[word] = packed;
    }

    public static void widen(final long[] source, final int sourceBits,
                             final long[] destination, final int destinationBits, final int size) {
        final long sourceMask = (1L << sourceBits) - 1;
        int sourceBit = 0;
        int destinationWord = 0;
        int destinationOffset = 0;
        long packed = 0;
        for (int index = 0; index < size; index++, sourceBit += sourceBits) {
            final int sourceWord = sourceBit >>> 6;
            final int sourceOffset = sourceBit & 63;
            long id = source[sourceWord] >>> sourceOffset;
            if (sourceOffset + sourceBits > 64) id |= source[sourceWord + 1] << (64 - sourceOffset);
            id &= sourceMask;
            packed |= id << destinationOffset;
            final int end = destinationOffset + destinationBits;
            if (end >= 64) {
                destination[destinationWord++] = packed;
                packed = end == 64 ? 0 : id >>> (64 - destinationOffset);
                destinationOffset = end - 64;
            } else {
                destinationOffset = end;
            }
        }
        if (destinationOffset != 0) destination[destinationWord] = packed;
    }
}
