package github.kasuminova.novaeng.client.memory;

import java.nio.IntBuffer;

/**
 * Transfers only the current upload slice; anaglyph never clones the whole source image.
 */
public final class TexturePixelTransfer {
    private TexturePixelTransfer() {
    }

    public static void copy(final IntBuffer target, final int[] source, final int offset, final int length,
                            final boolean anaglyph) {
        target.clear();
        if (anaglyph) {
            for (int i = offset; i < offset + length; i++) {
                final int color = source[i];
                final int alpha = color >>> 24;
                final int red = color >> 16 & 255;
                final int green = color >> 8 & 255;
                final int blue = color & 255;
                target.put(alpha << 24 | (red * 30 + green * 59 + blue * 11) / 100 << 16
                    | (red * 30 + green * 70) / 100 << 8 | (red * 30 + blue * 70) / 100);
            }
        } else {
            target.put(source, offset, length);
        }
        target.flip();
    }
}
