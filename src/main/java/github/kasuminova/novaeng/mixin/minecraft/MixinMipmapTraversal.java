package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.client.renderer.texture.TextureUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TextureUtil.class)
public abstract class MixinMipmapTraversal {

    @Shadow
    private static int blendColors(final int a, final int b, final int c, final int d, final boolean alpha) {
        throw new AssertionError();
    }

    /**
     * @author circulation
     * @reason Traverse adjacent pixels together while retaining the installed color blender and supplied mip levels.
     */
    @Overwrite
    public static int[][] generateMipmapData(final int levels, final int width, final int[][] supplied) {
        final int[][] result = new int[levels + 1][];
        result[0] = supplied[0];
        if (levels > 0) {
            boolean alpha = false;
            for (final int pixel : supplied[0]) {
                if (pixel >> 24 == 0) {
                    alpha = true;
                    break;
                }
            }
            for (int level = 1; level <= levels; level++) {
                if (supplied[level] != null) {
                    result[level] = supplied[level];
                    continue;
                }
                final int[] previous = result[level - 1];
                final int[] pixels = new int[previous.length >> 2];
                final int levelWidth = width >> level;
                if (levelWidth > 0) {
                    final int height = pixels.length / levelWidth;
                    final int stride = levelWidth << 1;
                    for (int y = 0; y < height; y++) {
                        final int sourceRow = 2 * y * stride;
                        final int targetRow = y * levelWidth;
                        for (int x = 0; x < levelWidth; x++) {
                            final int source = sourceRow + 2 * x;
                            pixels[targetRow + x] = blendColors(previous[source], previous[source + 1],
                                previous[source + stride], previous[source + stride + 1], alpha);
                        }
                    }
                }
                result[level] = pixels;
            }
        }
        return result;
    }
}
