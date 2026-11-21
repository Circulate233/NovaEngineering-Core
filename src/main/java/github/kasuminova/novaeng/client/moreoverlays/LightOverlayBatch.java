package github.kasuminova.novaeng.client.moreoverlays;

import at.feldim2425.moreoverlays.api.lightoverlay.ILightScanner;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Emits the original cross order and per-vertex colors in a single nonempty batch.
 */
public final class LightOverlayBatch {
    private LightOverlayBatch() {
    }

    public static void emit(final ILightScanner scanner, final int alwaysColor, final int nightColor, final Sink sink) {
        final float ar = (float) (alwaysColor >> 16 & 255) / 255.0f;
        final float ag = (float) (alwaysColor >> 8 & 255) / 255.0f;
        final float ab = (float) (alwaysColor & 255) / 255.0f;
        final float nr = (float) (nightColor >> 16 & 255) / 255.0f;
        final float ng = (float) (nightColor >> 8 & 255) / 255.0f;
        final float nb = (float) (nightColor & 255) / 255.0f;
        boolean started = false;
        try {
            final LightScanCache cache = scanner instanceof PrimitiveLightScanner primitive
                ? primitive.novaeng$primitiveLightModes() : null;
            if (cache != null && !cache.isExposed()) {
                for (int i = 0; i < cache.size(); i++) {
                    final byte mode = cache.mode(i);
                    if (mode != 1 && mode != 2) continue;
                    if (!started) {
                        sink.begin();
                        started = true;
                    }
                    cross(sink, cache.x(i), cache.y(i), cache.z(i), mode == 1 ? nr : ar, mode == 1 ? ng : ag, mode == 1 ? nb : ab);
                }
            } else {
                for (Pair<BlockPos, Byte> entry : scanner.getLightModes()) {
                    final Byte mode = entry.getValue();
                    if (mode == null || mode != 1 && mode != 2) continue;
                    final BlockPos pos = entry.getKey();
                    // Resolve getters in the original order before beginning, including null-position failures.
                    final int y = pos.getY(), x = pos.getX(), z = pos.getZ();
                    if (!started) {
                        sink.begin();
                        started = true;
                    }
                    cross(sink, x, y, z, mode == 1 ? nr : ar, mode == 1 ? ng : ag, mode == 1 ? nb : ab);
                }
            }
        } finally {
            // A custom scanner may throw during iteration; finish any pending vertices before propagating it.
            if (started) sink.end();
        }
    }

    private static void cross(final Sink sink, final int x, final int y, final int z,
                              final float r, final float g, final float b) {
        final double height = (double) y + 0.005;
        final double x1 = (double) x + 1.0;
        final double z1 = (double) z + 1.0;
        sink.vertex(x, height, z, r, g, b);
        sink.vertex(x1, height, z1, r, g, b);
        sink.vertex(x1, height, z, r, g, b);
        sink.vertex(x, height, z1, r, g, b);
    }

    public interface Sink {
        void begin();

        void vertex(double x, double y, double z, float red, float green, float blue);

        void end();
    }
}
