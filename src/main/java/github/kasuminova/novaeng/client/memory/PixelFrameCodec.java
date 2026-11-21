package github.kasuminova.novaeng.client.memory;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/** Lossless LZ4 block coding of little-endian pixel words, without native storage or decode temporaries. */
public final class PixelFrameCodec {
    private PixelFrameCodec() { }

    private static int octet(final int[] words, final int offset) {
        return words[offset >>> 2] >>> ((offset & 3) * 8) & 255;
    }
    private static int word(final int[] words, final int offset) {
        final int shift = (offset & 3) * 8;
        final int first = words[offset >>> 2] >>> shift;
        return shift == 0 ? first : first | words[(offset >>> 2) + 1] << (32 - shift);
    }
    private static void octet(final int[] words, final int offset, final int value) {
        final int shift = (offset & 3) * 8;
        words[offset >>> 2] = words[offset >>> 2] & ~(255 << shift) | value << shift;
    }
    private static void length(final ByteArrayOutputStream out, int value) {
        while (value >= 255) { out.write(255); value -= 255; }
        out.write(value);
    }

    public static byte[] encode(final int[] pixels, final int[] dictionary) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(Math.multiplyExact(pixels.length, 4), 8192));
        encode(pixels, dictionary, out, Integer.MAX_VALUE);
        return out.toByteArray();
    }

    private static boolean encode(final int[] pixels, final int[] dictionary, final ByteArrayOutputStream out, final int limit) {
        if (dictionary.length != 16384) throw new IllegalArgumentException("dictionary size");
        Arrays.fill(dictionary, -1);
        final int size = Math.multiplyExact(pixels.length, 4);
        out.reset();
        int anchor = 0;
        int cursor = 0;
        while (cursor <= size - 12) {
            final int value = word(pixels, cursor);
            final int hash = value * -1640531535 >>> 18;
            final int match = dictionary[hash];
            dictionary[hash] = cursor;
            if (match < 0 || cursor - match > 65535 || word(pixels, match) != value) { cursor++; continue; }
            int end = cursor + 4;
            // Leave five final literals, as required by the LZ4 block format.
            while (end < size - 5 && octet(pixels, end) == octet(pixels, match + end - cursor)) end++;
            final int literals = cursor - anchor;
            final int matchLength = end - cursor - 4;
            final long sequenceBytes = 3L + literals + (literals >= 15 ? (literals - 15) / 255 + 1 : 0)
                + (matchLength >= 15 ? (matchLength - 15) / 255 + 1 : 0);
            if (out.size() + sequenceBytes > limit) return false;
            out.write(Math.min(literals, 15) << 4 | Math.min(matchLength, 15));
            if (literals >= 15) length(out, literals - 15);
            for (int i = anchor; i < cursor; i++) out.write(octet(pixels, i));
            final int distance = cursor - match;
            out.write(distance);
            out.write(distance >>> 8);
            if (matchLength >= 15) length(out, matchLength - 15);
            cursor = end;
            anchor = end;
        }
        final int literals = size - anchor;
        if (out.size() + 1L + literals + (literals >= 15 ? (literals - 15) / 255 + 1 : 0) > limit) return false;
        out.write(Math.min(literals, 15) << 4);
        if (literals >= 15) length(out, literals - 15);
        for (int i = anchor; i < size; i++) out.write(octet(pixels, i));
        return true;
    }

    /** One scratch buffer per packing pass; only useful payloads get their own retained byte array. */
    public static final class Encoder {
        private final int[] dictionary = new int[16384];
        private final ByteArrayOutputStream output = new ByteArrayOutputStream(8192);

        public byte[] encodeIfSmaller(final int[] pixels) {
            if (pixels.length <= 8) return null;
            final int limit = Math.multiplyExact(pixels.length, 4) - 33;
            return encode(pixels, dictionary, output, limit) ? output.toByteArray() : null;
        }
    }

    public static void decode(final byte[] input, final int[] pixels) {
        final int size = Math.multiplyExact(pixels.length, 4);
        int cursor = 0;
        int output = 0;
        while (cursor < input.length) {
            final int token = input[cursor++] & 255;
            int literals = token >>> 4;
            if (literals == 15) {
                int extra;
                do {
                    if (cursor >= input.length) throw new IllegalArgumentException("truncated literal length");
                    extra = input[cursor++] & 255;
                    literals = Math.addExact(literals, extra);
                } while (extra == 255);
            }
            if (literals > input.length - cursor || literals > size - output) throw new IllegalArgumentException("literal overrun");
            final int literalEnd = output + literals;
            while ((output & 3) != 0 && output < literalEnd) octet(pixels, output++, input[cursor++] & 255);
            while (literalEnd - output >= 4) {
                pixels[output >>> 2] = (input[cursor] & 255) | (input[cursor + 1] & 255) << 8
                    | (input[cursor + 2] & 255) << 16 | input[cursor + 3] << 24;
                output += 4;
                cursor += 4;
            }
            while (output < literalEnd) octet(pixels, output++, input[cursor++] & 255);
            if (cursor == input.length) break;
            if (input.length - cursor < 2) throw new IllegalArgumentException("truncated offset");
            final int distance = (input[cursor] & 255) | (input[cursor + 1] & 255) << 8;
            cursor += 2;
            if (distance == 0 || distance > output) throw new IllegalArgumentException("invalid offset");
            int count = (token & 15) + 4;
            if ((token & 15) == 15) {
                int extra;
                do {
                    if (cursor >= input.length) throw new IllegalArgumentException("truncated match length");
                    extra = input[cursor++] & 255;
                    count = Math.addExact(count, extra);
                } while (extra == 255);
            }
            if (count > size - output) throw new IllegalArgumentException("match overrun");
            final int matchEnd = output + count;
            while ((output & 3) != 0 && output < matchEnd) { octet(pixels, output, octet(pixels, output - distance)); output++; }
            if (distance >= 4) {
                if ((distance & 3) == 0) {
                    // Copy only initialized words. Doubling the available run preserves LZ4 overlap semantics.
                    final int source = (output - distance) >>> 2;
                    while (matchEnd - output >= 4) {
                        final int words = Math.min((matchEnd - output) >>> 2, (output >>> 2) - source);
                        System.arraycopy(pixels, source, pixels, output >>> 2, words);
                        output += words * 4;
                    }
                } else {
                    while (matchEnd - output >= 4) {
                        pixels[output >>> 2] = word(pixels, output - distance);
                        output += 4;
                    }
                }
            }
            while (output < matchEnd) { octet(pixels, output, octet(pixels, output - distance)); output++; }
        }
        if (output != size) throw new IllegalArgumentException("incomplete pixel block");
    }
}
