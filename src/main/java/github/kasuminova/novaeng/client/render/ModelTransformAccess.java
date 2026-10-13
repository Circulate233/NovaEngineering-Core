package github.kasuminova.novaeng.client.render;

import github.kasuminova.novaeng.common.util.ReflectiveAccess;
import net.minecraftforge.common.model.ITransformation;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.vecmath.Matrix4f;
import java.lang.invoke.MethodHandle;

public final class ModelTransformAccess {

    private static final MethodHandle MATRIX = ReflectiveAccess.getter(TRSRTransformation.class, "matrix", Matrix4f.class);

    private ModelTransformAccess() {
    }

    public static Matrix4f readOnlyMatrix(final ITransformation transformation) {
        if (transformation.getClass() != TRSRTransformation.class) {
            return transformation.getMatrix();
        }
        try {
            return (Matrix4f) MATRIX.invokeExact((TRSRTransformation) transformation);
        } catch (Throwable exception) {
            throw new IllegalStateException("Unable to read model transformation matrix", exception);
        }
    }
}
