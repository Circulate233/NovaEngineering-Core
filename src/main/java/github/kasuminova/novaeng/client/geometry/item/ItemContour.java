package github.kasuminova.novaeng.client.geometry.item;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Immutable ordered Forge item edges, independent of atlas, tint and model.
 */
public final class ItemContour {
    private final int width;
    private final int height;
    private final short[] small;
    private final int[] large;
    private final int hash;

    private ItemContour(final int width, final int height, final int[] words, final int count) {
        this.width = width;
        this.height = height;
        if (width <= 16 && height <= 16) {
            small = new short[count];
            for (int i = 0; i < count; i++) small[i] = (short) words[i];
            large = null;
        } else {
            small = null;
            large = Arrays.copyOf(words, count);
        }
        hash = 31 * (31 * width + height) + (small == null ? Arrays.hashCode(large) : Arrays.hashCode(small));
    }

    /**
     * Pixels are mip level zero, in the original image order, for every animation frame.
     */
    public static ItemContour create(final int width, final int height, final int[][] frames) {
        return create(width, height, frames.length, frames, null);
    }

    /** Consumes each borrowed mipmap frame completely before asking for the next one. */
    public static ItemContour createFromMipmaps(final int width, final int height, final List<int[][]> frames) {
        return createFromMipmaps(width, height, frames.size(), frames);
    }

    public static ItemContour createFromMipmaps(final int width, final int height, final int frameCount,
                                                final List<int[][]> frames) {
        return create(width, height, frameCount, null, frames);
    }

    private static ItemContour create(final int width, final int height, final int frameCount,
                                      final int[][] frames, final List<int[][]> mipmaps) {
        if (width < 1 || height < 1 || width > 256 || height > 256) return null;
        // Frame caches may already have been released at bake time. Forge then emits only its two caps.
        if (frameCount == 0) return new ItemContour(width, height, new int[0], 0);
        // Dense pixel flags are bit vectors, rather than boxed or primitive-key collections.
        final BitSet[] edges = {new BitSet(), new BitSet(), new BitSet(), new BitSet()};
        final boolean[] above = new boolean[width];
        boolean translucent = false;
        for (int frame = 0; frame < frameCount; frame++) {
            final int[] pixels;
            if (mipmaps == null) {
                pixels = frames[frame];
            } else {
                final int[][] levels = mipmaps.get(frame);
                pixels = levels == null || levels.length == 0 ? null : levels[0];
            }
            if (pixels == null || pixels.length < width * height) return null;
            Arrays.fill(above, true);
            for (int y = 0; y < height; y++) {
                boolean left = true;
                for (int x = 0; x < width; x++) {
                    final int alpha = pixels[x + (height - 1 - y) * width] >>> 24;
                    final boolean transparent = alpha / 255f <= .1f;
                    translucent |= !transparent && alpha < 255;
                    // Forge's FaceData uses height as its stride, including for rectangular sprites.
                    if (left && !transparent) edges[2].set(y * height + x);
                    if (!left && transparent) edges[3].set(y * height + x - 1);
                    if (above[x] && !transparent) edges[0].set(y * height + x);
                    if (!above[x] && transparent) edges[1].set((y - 1) * height + x);
                    left = transparent;
                    above[x] = transparent;
                }
                if (!left) edges[3].set(y * height + width - 1);
            }
            for (int x = 0; x < width; x++) if (!above[x]) edges[1].set((height - 1) * height + x);
        }
        final int bits = width <= 16 && height <= 16 ? 4 : 8;
        int[] words = new int[32];
        int count = 0;
        for (int direction = 0; direction < 4; direction++) {
            final boolean horizontal = direction < 2;
            final int rows = horizontal ? height : width;
            final int length = horizontal ? width : height;
            for (int row = 0; row < rows; row++) {
                int start = -1;
                int end = length;
                for (int pixel = 0; pixel < length; pixel++) {
                    final int index = horizontal ? row * height + pixel : pixel * height + row;
                    final boolean edge = edges[direction].get(index);
                    if (!translucent) {
                        if (edge) {
                            if (start < 0) start = pixel;
                            end = pixel + 1;
                        }
                    } else if (start >= 0 && !edge) {
                        if (count == words.length) words = Arrays.copyOf(words, count * 2);
                        words[count++] = encode(direction, row, start, pixel - start, bits);
                        start = -1;
                    } else if (start < 0 && edge) {
                        start = pixel;
                    }
                }
                if (start >= 0) {
                    if (count == words.length) words = Arrays.copyOf(words, count * 2);
                    words[count++] = encode(direction, row, start, end - start, bits);
                }
            }
        }
        return new ItemContour(width, height, words, count);
    }

    private static int encode(final int direction, final int row, final int start, final int length, final int bits) {
        return direction << (bits * 3) | row << (bits * 2) | start << bits | length - 1;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int bits() {
        return small == null ? 8 : 4;
    }

    public int size() {
        return small == null ? large.length : small.length;
    }

    public int word(final int index) {
        return small == null ? large[index] : Short.toUnsignedInt(small[index]);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof ItemContour that)) return false;
        return width == that.width && height == that.height && Arrays.equals(small, that.small) && Arrays.equals(large, that.large);
    }
}
