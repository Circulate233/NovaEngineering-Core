package github.kasuminova.novaeng.client.geometry.vertex;

import java.util.Arrays;

/**
 * Immutable four-vertex float data, including components which the packed format omits.
 */
public final class LosslessFloatRecord {
    private final int elements;
    private final long modes;
    private final int[] words;

    private LosslessFloatRecord(final int elements, final long modes, final int[] words) {
        this.elements = elements;
        this.modes = modes;
        this.words = words;
    }

    public static LosslessFloatRecord capture(final float[] data, final int elements) {
        if (elements < 0 || data.length != Math.multiplyExact(elements, 16)) {
            throw new IllegalArgumentException("Four vertices with four components per element required");
        }
        return capture(data, null, elements);
    }

    static LosslessFloatRecord captureTree(final float[][][] data, final int elements) {
        return capture(null, data, elements);
    }

    private static LosslessFloatRecord capture(final float[] flat, final float[][][] tree, final int elements) {
        if (elements > 8) return raw(flat, tree, elements);
        long modes = 0;
        int length = 0;
        for (int channel = 0; channel < elements * 4; channel++) {
            final int first = bits(flat, tree, elements, 0, channel);
            boolean same = true;
            for (int v = 1; v < 4; v++) same &= bits(flat, tree, elements, v, channel) == first;
            final int mode = same ? (first == 0 ? 0 : 1) : 2;
            modes |= (long) mode << (channel * 2);
            length += mode == 2 ? 4 : mode;
        }
        if (length >= elements * 16) return raw(flat, tree, elements);
        final int[] compact = new int[length];
        int offset = 0;
        for (int channel = 0; channel < elements * 4; channel++) {
            final int mode = (int) (modes >>> (channel * 2)) & 3;
            if (mode == 1) compact[offset++] = bits(flat, tree, elements, 0, channel);
            else if (mode == 2) {
                for (int v = 0; v < 4; v++) compact[offset++] = bits(flat, tree, elements, v, channel);
            }
        }
        return new LosslessFloatRecord(elements, modes, compact);
    }

    private static LosslessFloatRecord raw(final float[] flat, final float[][][] tree, final int elements) {
        final int[] words = new int[elements * 16];
        for (int v = 0; v < 4; v++)
            for (int channel = 0; channel < elements * 4; channel++)
                words[v * elements * 4 + channel] = bits(flat, tree, elements, v, channel);
        return new LosslessFloatRecord(elements, -1L, words);
    }

    private static int bits(final float[] flat, final float[][][] tree, final int elements, final int vertex, final int channel) {
        return Float.floatToRawIntBits(flat == null ? tree[vertex][channel >>> 2][channel & 3] : flat[vertex * elements * 4 + channel]);
    }

    private int channelOffset(final int channel) {
        final long prefix = modes & ((1L << (channel * 2)) - 1);
        return Long.bitCount(prefix & 0x5555555555555555L) + Long.bitCount(prefix & 0xAAAAAAAAAAAAAAAAL) * 4;
    }

    public int elementCount() {
        return elements;
    }

    public int storedWordCount() {
        return words.length;
    }

    public boolean isRaw() {
        return modes == -1L;
    }

    public int rawBits(final int vertex, final int element, final int component) {
        if (vertex < 0 || vertex >= 4 || element < 0 || element >= elements || component < 0 || component >= 4) {
            throw new IndexOutOfBoundsException();
        }
        final int channel = element * 4 + component;
        if (isRaw()) return words[vertex * elements * 4 + channel];
        final int offset = channelOffset(channel);
        final int mode = (int) (modes >>> (channel * 2)) & 3;
        return mode == 0 ? 0 : words[offset + (mode == 2 ? vertex : 0)];
    }

    public void elementInto(final int vertex, final int element, final float[] target) {
        if (vertex < 0 || vertex >= 4 || element < 0 || element >= elements) throw new IndexOutOfBoundsException();
        if (isRaw()) {
            final int base = (vertex * elements + element) << 2;
            for (int c = 0; c < 4; c++) target[c] = Float.intBitsToFloat(words[base + c]);
            return;
        }
        int offset = channelOffset(element * 4);
        for (int c = 0; c < 4; c++) {
            final int mode = (int) (modes >>> ((element * 4 + c) * 2)) & 3;
            target[c] = Float.intBitsToFloat(mode == 0 ? 0 : words[offset + (mode == 2 ? vertex : 0)]);
            offset += mode == 2 ? 4 : mode;
        }
    }

    public float[][][] materialize() {
        final float[][][] result = new float[4][elements][4];
        for (int v = 0; v < 4; v++) {
            for (int e = 0; e < elements; e++) elementInto(v, e, result[v][e]);
        }
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        return other == this || other instanceof LosslessFloatRecord that
            && elements == that.elements && modes == that.modes && Arrays.equals(words, that.words);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * elements + Long.hashCode(modes)) + Arrays.hashCode(words);
    }
}
