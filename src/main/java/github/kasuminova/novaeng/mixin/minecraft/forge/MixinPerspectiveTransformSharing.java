package github.kasuminova.novaeng.mixin.minecraft.forge;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.novaeng.client.model.OwnedModelTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.TRSRTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PerspectiveMapWrapper.class, remap = false)
public abstract class MixinPerspectiveTransformSharing {
    @Inject(method = {
        "getTransforms(Lnet/minecraftforge/common/model/IModelState;)Lcom/google/common/collect/ImmutableMap;",
        "getTransforms(Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms;)Lcom/google/common/collect/ImmutableMap;"
    }, at = @At("RETURN"), cancellable = true, require = 2)
    private static void nova$shareOwnedTransforms(final CallbackInfoReturnable<ImmutableMap<TransformType, TRSRTransformation>> cir) {
        cir.setReturnValue(OwnedModelTransforms.share(cir.getReturnValue()));
    }
}
