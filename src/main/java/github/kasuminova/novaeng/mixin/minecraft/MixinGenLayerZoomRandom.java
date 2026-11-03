package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GenLayerZoom.class)
public abstract class MixinGenLayerZoomRandom extends GenLayer {

    protected MixinGenLayerZoomRandom(final long seed) {
        super(seed);
    }

    @Inject(method = "getInts(IIII)[I", at = @At("HEAD"), cancellable = true, require = 1)
    private void nova$fixedRandomChoices(final int areaX, final int areaY, final int width, final int height,
                                         final CallbackInfoReturnable<int[]> cir) {
        final Class<?> type = this.getClass();
        final boolean fuzzy = type == GenLayerFuzzyZoom.class;
        if (!fuzzy && type != GenLayerZoom.class) {
            return;
        }
        final int parentX = areaX >> 1;
        final int parentY = areaY >> 1;
        final int parentWidth = (width >> 1) + 2;
        final int parentHeight = (height >> 1) + 2;
        final int[] input = this.parent.getInts(parentX, parentY, parentWidth, parentHeight);
        final int expandedWidth = (parentWidth - 1) << 1;
        final int expandedHeight = (parentHeight - 1) << 1;
        final int[] expanded = IntCache.getIntCache(expandedWidth * expandedHeight);
        for (int y = 0; y < parentHeight - 1; y++) {
            int output = (y << 1) * expandedWidth;
            int upperLeft = input[y * parentWidth];
            int lowerLeft = input[(y + 1) * parentWidth];
            for (int x = 0; x < parentWidth - 1; x++) {
                final int seedX = (x + parentX) << 1;
                final int seedY = (y + parentY) << 1;
                this.initChunkSeed(seedX, seedY);
                final int upperRight = input[x + 1 + y * parentWidth];
                final int lowerRight = input[x + 1 + (y + 1) * parentWidth];
                expanded[output] = upperLeft;
                expanded[output++ + expandedWidth] = this.nextInt(2) == 0 ? upperLeft : lowerLeft;
                expanded[output] = this.nextInt(2) == 0 ? upperLeft : upperRight;
                expanded[output++ + expandedWidth] = fuzzy ? this.nova$chooseFour(upperLeft, upperRight, lowerLeft, lowerRight)
                    : this.nova$modeOrRandom(upperLeft, upperRight, lowerLeft, lowerRight);
                upperLeft = upperRight;
                lowerLeft = lowerRight;
            }
        }
        // Keep both IntCache requests and the original expanded layout, including its padding behavior.
        final int[] result = IntCache.getIntCache(width * height);
        for (int y = 0; y < height; y++) {
            System.arraycopy(expanded, (y + (areaY & 1)) * expandedWidth + (areaX & 1), result, y * width, width);
        }
        cir.setReturnValue(result);
    }

    @Unique
    private int nova$modeOrRandom(final int a, final int b, final int c, final int d) {
        if (b == c && c == d) {
            return b;
        } else if (a == b && a == c) {
            return a;
        } else if (a == b && a == d) {
            return a;
        } else if (a == c && a == d) {
            return a;
        } else if (a == b && c != d) {
            return a;
        } else if (a == c && b != d) {
            return a;
        } else if (a == d && b != c) {
            return a;
        } else if (b == c && a != d) {
            return b;
        } else if (b == d && a != c) {
            return b;
        }
        return c == d && a != b ? c : this.nova$chooseFour(a, b, c, d);
    }

    @Unique
    private int nova$chooseFour(final int a, final int b, final int c, final int d) {
        return switch (this.nextInt(4)) {
            case 0 -> a;
            case 1 -> b;
            case 2 -> c;
            default -> d;
        };
    }
}
