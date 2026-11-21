package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.client.model.OwnedModelTransforms;
import net.minecraftforge.common.model.TRSRTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

@Mixin(value = TRSRTransformation.class, remap = false)
public abstract class MixinOwnedTransformFactories {
    @Shadow @Final private Matrix4f matrix;
    @Shadow private boolean full;
    @Shadow private Vector3f translation;
    @Shadow private Quat4f leftRot;
    @Shadow private Vector3f scale;
    @Shadow private Quat4f rightRot;
    @Shadow private Matrix3f normalTransform;

    // Shared transforms lazily compute decomposition and normal data. Publish each complete result under one monitor.
    /** @author NovaEngineering @reason Synchronize the actual lazy initializer without an operation wrapper. */
    @Overwrite
    private synchronized void genCheck() {
        if (!full) {
            final Pair<Matrix3f, Vector3f> affine = TRSRTransformation.toAffine(matrix);
            final Triple<Quat4f, Vector3f, Quat4f> decomposed = TRSRTransformation.svdDecompose(affine.getLeft());
            translation = affine.getRight();
            leftRot = decomposed.getLeft();
            scale = decomposed.getMiddle();
            rightRot = decomposed.getRight();
            full = true;
        }
    }

    /** @author NovaEngineering @reason Synchronize the actual lazy initializer without an operation wrapper. */
    @Overwrite
    private synchronized void checkNormalTransform() {
        if (normalTransform == null) {
            normalTransform = new Matrix3f();
            matrix.getRotationScale(normalTransform);
            normalTransform.invert();
            normalTransform.transpose();
        }
    }

    @Redirect(method = {"compose", "inverse", "blockCenterToCorner", "blockCornerToCenter"},
        at = @At(value = "NEW", target = "(Ljavax/vecmath/Matrix4f;)Lnet/minecraftforge/common/model/TRSRTransformation;"), require = 4)
    private static TRSRTransformation nova$sharePrivateFactoryMatrix(final Matrix4f matrix) {
        return OwnedModelTransforms.create(matrix);
    }
}
