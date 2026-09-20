package github.kasuminova.novaeng.mixin.libnine;

import github.kasuminova.novaeng.client.resource.ModelJsonMetadataIndex;
import github.kasuminova.novaeng.client.resource.ModelJsonMetadataIndexImpl;
import io.github.phantamanta44.libnine.client.model.L9Models;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Answers LibNine model-type probes from the shared generation-scoped JSON metadata index.
 */
@Mixin(value = L9Models.class, priority = 2000, remap = false)
public abstract class MixinL9Models {

    @Inject(method = "isOfType", at = @At("HEAD"), cancellable = true,
        remap = false, require = 1)
    private static void nova$readSharedModelType(final ResourceLocation resource,
                                                 final String type,
                                                 final CallbackInfoReturnable<Boolean> cir) {
        final ModelJsonMetadataIndex metadata = ModelJsonMetadataIndexImpl.instance();
        if (metadata.isActive()) {
            cir.setReturnValue(metadata.isLibNineType(resource, type));
        }
    }
}
