package github.kasuminova.novaeng.client.font;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontProjectionTest {
    private static float[] transform(final float[] m, final float x, final float y, final float z, final float w) {
        return new float[]{m[0] * x + m[4] * y + m[8] * z + m[12] * w,
            m[1] * x + m[5] * y + m[9] * z + m[13] * w,
            m[2] * x + m[6] * y + m[10] * z + m[14] * w,
            m[3] * x + m[7] * y + m[11] * z + m[15] * w};
    }

    private static float[] referenceProject(final float[] m, final float[] p, final float x, final float y,
                                            final int width, final int height, final boolean visible) {
        final float[] eye = transform(m, x, y, 0F, 1F), clip = transform(p, eye[0], eye[1], eye[2], eye[3]);
        if (!Float.isFinite(clip[3]) || (visible ? clip[3] <= 0F : Math.abs(clip[3]) < 1E-6F)) return null;
        final float invW = 1F / clip[3];
        return new float[]{clip[0] * invW * width * 0.5F, clip[1] * invW * height * 0.5F};
    }

    private static float referenceScale(final float[] m, final float[] p, final int width, final int height) {
        final float[] a = referenceProject(m, p, 0F, 0F, width, height, false);
        final float[] b = referenceProject(m, p, 1F, 0F, width, height, false);
        final float[] c = referenceProject(m, p, 0F, 1F, width, height, false);
        if (a == null || b == null || c == null) return Float.NaN;
        final float bx = b[0] - a[0], by = b[1] - a[1], cx = c[0] - a[0], cy = c[1] - a[1];
        final float scale = ((float) Math.sqrt(bx * bx + by * by) + (float) Math.sqrt(cx * cx + cy * cy)) * 0.5F;
        return Float.isFinite(scale) ? scale : Float.NaN;
    }

    private static boolean referenceVisible(final float[] m, final float[] p, final float x, final float y, final float width,
                                            final float height, final float minPixelHeight, final int displayWidth, final int displayHeight) {
        final float[][] corners = {referenceProject(m, p, x, y, displayWidth, displayHeight, true),
            referenceProject(m, p, x + width, y, displayWidth, displayHeight, true),
            referenceProject(m, p, x, y + height, displayWidth, displayHeight, true),
            referenceProject(m, p, x + width, y + height, displayWidth, displayHeight, true)};
        float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (final float[] corner : corners) {
            if (corner == null) return true;
            minX = Math.min(minX, corner[0]);
            minY = Math.min(minY, corner[1]);
            maxX = Math.max(maxX, corner[0]);
            maxY = Math.max(maxY, corner[1]);
        }
        final float halfWidth = displayWidth * 0.5F, halfHeight = displayHeight * 0.5F;
        return !(maxX < -halfWidth) && !(minX > halfWidth) && !(maxY < -halfHeight) && !(minY > halfHeight)
            && maxY - minY >= Math.max(0F, minPixelHeight);
    }

    @Test
    void scalarScaleAndVisibilityMatchTheOriginalArrayMathIncludingPerspective() {
        final Random random = new Random(0x19_11_2026L);
        for (int trial = 0; trial < 2000; trial++) {
            final float[] modelview = new float[16], projection = new float[16];
            for (int i = 0; i < 16; i++) {
                modelview[i] = random.nextFloat() * 4F - 2F;
                projection[i] = random.nextFloat() * 4F - 2F;
            }
            if (trial % 20 == 0) projection[15] = 0F;
            if (trial % 39 == 0) modelview[1] = Float.NaN;
            if (trial % 37 == 0) projection[15] = Float.POSITIVE_INFINITY;
            final float expectedScale = referenceScale(modelview, projection, 1920, 1080);
            final float actualScale = FontProjection.pixelScale(modelview, projection, 1920, 1080);
            assertEquals(Float.floatToIntBits(expectedScale), Float.floatToIntBits(actualScale));
            assertEquals(referenceVisible(modelview, projection, -0.5F, 0.1F, 0.3F, 0.4F, 2F, 1920, 1080),
                FontProjection.visible(modelview, projection, -0.5F, 0.1F, 0.3F, 0.4F, 2F, 1920, 1080));
        }
        assertTrue(Float.isNaN(FontProjection.pixelScale(null, new float[16], 1920, 1080)));
        assertTrue(Float.isNaN(FontProjection.pixelScale(new float[16], new float[16], 0, 1080)));
    }

    @Test
    void renderThreadOriginStorageIsReusedAndEachResultIsConsumedImmediately() {
        final float[] identity = new float[16];
        identity[0] = identity[5] = identity[10] = identity[15] = 1F;
        final float[] first = FontProjection.origin(identity, identity, 1F, 2F, 0F, 200, 100);
        assertArrayEquals(new float[]{100F, 100F}, first);
        assertSame(first, FontProjection.origin(identity, identity, 7F, 8F, 0F, 200, 100));
        assertArrayEquals(new float[]{700F, 400F}, first);
    }
}
