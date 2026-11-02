package github.kasuminova.novaeng.client.model;

import javax.vecmath.Matrix4f;

/**
 * The three checked columns of TRSRTransformation.isInteger, without its temporary matrix.
 */
public final class ModelTransformChecks {

    private ModelTransformChecks() {
    }

    public static boolean isInteger(final Matrix4f matrix) {
        final float w0 = matrix.m30 + matrix.m31 * 0.0F + matrix.m32 * 0.0F + matrix.m33;
        final float w1 = matrix.m30 * 0.0F + matrix.m31 + matrix.m32 * 0.0F + matrix.m33;
        final float w2 = matrix.m30 * 0.0F + matrix.m31 * 0.0F + matrix.m32 + matrix.m33;
        return integerRow(matrix.m00, matrix.m01, matrix.m02, matrix.m03, w0, w1, w2)
            && integerRow(matrix.m10, matrix.m11, matrix.m12, matrix.m13, w0, w1, w2)
            && integerRow(matrix.m20, matrix.m21, matrix.m22, matrix.m23, w0, w1, w2);
    }

    private static boolean integerRow(final float x, final float y, final float z, final float translation,
                                      final float w0, final float w1, final float w2) {
        // Preserve the multiplication/addition order, including zero * infinity and signed zero.
        return closeToInteger((x + y * 0.0F + z * 0.0F + translation) / w0)
            && closeToInteger((x * 0.0F + y + z * 0.0F + translation) / w1)
            && closeToInteger((x * 0.0F + y * 0.0F + z + translation) / w2);
    }

    private static boolean closeToInteger(final float value) {
        // The negated comparison deliberately retains the original result for NaN.
        return !((double) Math.abs(value - (float) Math.round(value)) > 1.0E-5D);
    }
}
