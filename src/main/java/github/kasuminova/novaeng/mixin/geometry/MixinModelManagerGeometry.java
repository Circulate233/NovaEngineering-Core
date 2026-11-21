package github.kasuminova.novaeng.mixin.geometry;

import github.kasuminova.novaeng.client.geometry.GeometryGeneration;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelManager.class)
public abstract class MixinModelManagerGeometry {
    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void novaeng$beginGeometryGeneration(final IResourceManager resources, final CallbackInfo ci) {
        GeometryGeneration.begin();
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void novaeng$endGeometryGeneration(final IResourceManager resources, final CallbackInfo ci) {
        GeometryGeneration.endCurrent();
    }
}
