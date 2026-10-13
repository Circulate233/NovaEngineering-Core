package github.kasuminova.novaeng.client.render;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public final class FaceBakeryPartRotation {

    private static final float SCALE_22_5 = 1.0F / (float) Math.cos((float) (Math.PI / 8)) - 1.0F;
    private static final float SCALE_GENERAL = 1.0F / (float) Math.cos((float) (Math.PI / 4)) - 1.0F;

    private final Matrix4f matrix = new Matrix4f();
    private final Vector3f axis = new Vector3f();
    private final Vector3f scale = new Vector3f();
    private final Vector4f vertex = new Vector4f();
    private int cachedAxis = -1;
    private int cachedAngle;
    private boolean cachedRescale;

    public void rotate(final Vector3f position, final Vector3f origin, final int axisIndex,
                       final float angle, final boolean rescale) {
        final int angleBits = Float.floatToRawIntBits(angle);
        if (cachedAxis != axisIndex || cachedAngle != angleBits || cachedRescale != rescale) {
            matrix.setIdentity();
            switch (axisIndex) {
                case 0 -> {
                    axis.set(1.0F, 0.0F, 0.0F);
                    scale.set(0.0F, 1.0F, 1.0F);
                }
                case 1 -> {
                    axis.set(0.0F, 1.0F, 0.0F);
                    scale.set(1.0F, 0.0F, 1.0F);
                }
                case 2 -> {
                    axis.set(0.0F, 0.0F, 1.0F);
                    scale.set(1.0F, 1.0F, 0.0F);
                }
                default -> throw new IllegalArgumentException("Unknown rotation axis: " + axisIndex);
            }
            Matrix4f.rotate(angle * (float) (Math.PI / 180.0), axis, matrix, matrix);
            if (rescale) {
                scale.scale(Math.abs(angle) == 22.5F ? SCALE_22_5 : SCALE_GENERAL);
                scale.set(scale.x + 1.0F, scale.y + 1.0F, scale.z + 1.0F);
            } else {
                scale.set(1.0F, 1.0F, 1.0F);
            }
            cachedAxis = axisIndex;
            cachedAngle = angleBits;
            cachedRescale = rescale;
        }
        vertex.set(position.x - origin.x, position.y - origin.y, position.z - origin.z, 1.0F);
        Matrix4f.transform(matrix, vertex, vertex);
        vertex.x = vertex.x * scale.x;
        vertex.y = vertex.y * scale.y;
        vertex.z = vertex.z * scale.z;
        position.set(vertex.x + origin.x, vertex.y + origin.y, vertex.z + origin.z);
    }
}
