package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.render.FaceBakeryPartRotation;
import github.kasuminova.novaeng.client.render.ModelTransformAccess;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraftforge.common.model.ITransformation;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.vecmath.Matrix4f;

@Mixin(FaceBakery.class)
public abstract class MixinFaceBakeryAllocations {

    @Unique
    private static final ThreadLocal<FaceBakeryPartRotation> NOVA_PART_ROTATION =
        ThreadLocal.withInitial(FaceBakeryPartRotation::new);

    @Unique
    private static final ThreadLocal<Vector3f> NOVA_VERTEX =
        ThreadLocal.withInitial(Vector3f::new);


    /**
     * Vanilla creates one short-lived Vector3f for each of the four vertices of every quad.
     * FaceBakery fills and consumes that value synchronously, so a per-loader-thread scratch
     * vector removes the allocation without retaining model data or changing the calculation.
     */
    @Redirect(method = "fillVertexData([IILnet/minecraft/util/EnumFacing;Lnet/minecraft/client/renderer/block/model/BlockFaceUV;[FLnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraftforge/common/model/ITransformation;Lnet/minecraft/client/renderer/block/model/BlockPartRotation;Z)V",
        at = @At(value = "NEW", target = "org/lwjgl/util/vector/Vector3f"),
        require = 1)
    private Vector3f nova$reuseVertex(final float x, final float y, final float z) {
        final Vector3f vertex = NOVA_VERTEX.get();
        vertex.set(x, y, z);
        return vertex;
    }

    @Inject(method = "rotatePart(Lorg/lwjgl/util/vector/Vector3f;Lnet/minecraft/client/renderer/block/model/BlockPartRotation;)V",
        at = @At("HEAD"), cancellable = true, require = 1)
    private void nova$reusePartRotation(final Vector3f position, final BlockPartRotation rotation,
                                        final CallbackInfo callbackInfo) {
        if (rotation != null) {
            NOVA_PART_ROTATION.get().rotate(position, rotation.origin, rotation.axis.ordinal(),
                rotation.angle, rotation.rescale);
            callbackInfo.cancel();
        }
    }

    @Redirect(method = "rotateVertex(Lorg/lwjgl/util/vector/Vector3f;Lnet/minecraft/util/EnumFacing;ILnet/minecraftforge/common/model/ITransformation;)I",
        at = @At(value = "INVOKE", target = "Lnet/minecraftforge/common/model/ITransformation;getMatrix()Ljavax/vecmath/Matrix4f;", remap = false),
        require = 1)
    private Matrix4f nova$readModelMatrix(final ITransformation transformation) {
        return ModelTransformAccess.readOnlyMatrix(transformation);
    }
}
