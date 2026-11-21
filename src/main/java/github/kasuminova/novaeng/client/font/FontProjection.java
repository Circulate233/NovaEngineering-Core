package github.kasuminova.novaeng.client.font;

/**
 * Scalar equivalents of the font renderer's short-lived matrix/vector arrays.
 */
public final class FontProjection {
    // pack() canonicalizes NaNs, so this noncanonical NaN pair cannot be a valid result.
    private static final long INVALID = 0x7F8000017F800001L;
    // The caller is render-thread-only and consumes both coordinates before any config callback.
    private static final float[] ORIGIN = new float[2];

    private FontProjection() {
    }

    public static float[] origin(final float[] modelview, final float[] projection, final float x, final float y, final float z,
                                 final int displayWidth, final int displayHeight) {
        final long point = project(modelview, projection, x, y, z, displayWidth, displayHeight, false);
        if (point == INVALID) return null;
        ORIGIN[0] = x(point);
        ORIGIN[1] = y(point);
        return ORIGIN;
    }

    public static float pixelScale(final float[] modelview, final float[] projection, final int width, final int height) {
        if (modelview == null || projection == null || modelview.length < 16 || projection.length < 16 || width <= 0 || height <= 0)
            return Float.NaN;
        final long origin = project(modelview, projection, 0F, 0F, 0F, width, height, false);
        final long xAxis = project(modelview, projection, 1F, 0F, 0F, width, height, false);
        final long yAxis = project(modelview, projection, 0F, 1F, 0F, width, height, false);
        if (origin == INVALID || xAxis == INVALID || yAxis == INVALID) return Float.NaN;
        final float scale = (distance(origin, xAxis) + distance(origin, yAxis)) * 0.5F;
        return Float.isFinite(scale) ? scale : Float.NaN;
    }

    public static boolean visible(final float[] modelview, final float[] projection, final float x, final float y, final float width,
                                  final float height, final float minPixelHeight, final int displayWidth, final int displayHeight) {
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 4; corner++) {
            final long point = project(modelview, projection, (corner & 1) == 0 ? x : x + width,
                (corner & 2) == 0 ? y : y + height, 0F, displayWidth, displayHeight, true);
            if (point == INVALID) return true;
            minX = Math.min(minX, x(point));
            minY = Math.min(minY, y(point));
            maxX = Math.max(maxX, x(point));
            maxY = Math.max(maxY, y(point));
        }
        final float halfWidth = displayWidth * 0.5F, halfHeight = displayHeight * 0.5F;
        return !(maxX < -halfWidth) && !(minX > halfWidth) && !(maxY < -halfHeight) && !(minY > halfHeight)
            && maxY - minY >= Math.max(0F, minPixelHeight);
    }

    private static long project(final float[] m, final float[] p, final float x, final float y, final float z,
                                final int width, final int height, final boolean frontOnly) {
        final float eyeX = m[0] * x + m[4] * y + m[8] * z + m[12];
        final float eyeY = m[1] * x + m[5] * y + m[9] * z + m[13];
        final float eyeZ = m[2] * x + m[6] * y + m[10] * z + m[14];
        final float eyeW = m[3] * x + m[7] * y + m[11] * z + m[15];
        final float clipX = p[0] * eyeX + p[4] * eyeY + p[8] * eyeZ + p[12] * eyeW;
        final float clipY = p[1] * eyeX + p[5] * eyeY + p[9] * eyeZ + p[13] * eyeW;
        final float clipW = p[3] * eyeX + p[7] * eyeY + p[11] * eyeZ + p[15] * eyeW;
        if (!Float.isFinite(clipW) || (frontOnly ? clipW <= 0F : Math.abs(clipW) < 1E-6F)) return INVALID;
        final float invW = 1F / clipW;
        final float screenX = clipX * invW * width * 0.5F, screenY = clipY * invW * height * 0.5F;
        return ((long) Float.floatToIntBits(screenY) << 32) | (Float.floatToIntBits(screenX) & 0xFFFFFFFFL);
    }

    private static float x(final long point) {
        return Float.intBitsToFloat((int) point);
    }

    private static float y(final long point) {
        return Float.intBitsToFloat((int) (point >>> 32));
    }

    private static float distance(final long a, final long b) {
        final float dx = x(b) - x(a), dy = y(b) - y(a);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

}
