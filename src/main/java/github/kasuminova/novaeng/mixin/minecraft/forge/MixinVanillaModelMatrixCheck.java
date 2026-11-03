package github.kasuminova.novaeng.mixin.minecraft.forge;

import github.kasuminova.novaeng.client.model.ModelTransformChecks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.vecmath.Matrix4f;

// VanillaModelWrapper is a private inner class of ModelLoader.
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper", remap = false)
public abstract class MixinVanillaModelMatrixCheck {

    @Redirect(method = "bakeNormal", at = @At(value = "INVOKE",
        target = "Lnet/minecraftforge/common/model/TRSRTransformation;isInteger(Ljavax/vecmath/Matrix4f;)Z"), require = 1)
    private boolean nova$checkIntegerColumns(final Matrix4f matrix) {
        return ModelTransformChecks.isInteger(matrix);
    }
}
