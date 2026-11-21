package github.kasuminova.novaeng.mixin.ae2;

import appeng.client.render.model.UVLModelLoader;
import github.kasuminova.novaeng.client.resource.ModelJsonMetadataIndex;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Answers AE2 UVL marker probes from generation-scoped model metadata.
 */
@Mixin(value = UVLModelLoader.class, priority = 2000, remap = false)
public abstract class MixinUVLModelLoader {

    @Inject(method = "accepts", at = @At("HEAD"), cancellable = true,
        remap = false, require = 1)
    private void nova$readSharedUvlMarker(final ResourceLocation modelLocation,
                                          final CallbackInfoReturnable<Boolean> cir) {
        final ModelJsonMetadataIndex metadata = ModelJsonMetadataIndex.instance();
        if (!metadata.isActive()) {
            return;
        }

        String modelPath = modelLocation.getPath();
        if (modelPath.startsWith("models/")) {
            modelPath = modelPath.substring("models/".length());
        }
        final ResourceLocation jsonLocation = new ResourceLocation(
            modelLocation.getNamespace(), "models/" + modelPath + ".json");
        cir.setReturnValue(metadata.hasUvlMarker(jsonLocation));
    }
}
