package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.resource.ModelJsonMetadataIndex;
import github.kasuminova.novaeng.client.resource.ModelJsonMetadataIndexImpl;
import github.kasuminova.novaeng.client.resource.SourcedResourceInputStream;
import github.kasuminova.stellarcore.mixin.util.StellarCoreResourcePack;
import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

/** Attaches the winning resource-pack identity to model JSON streams during an active metadata generation. */
@Mixin(FallbackResourceManager.class)
public abstract class MixinFallbackResourceManager {

    @Inject(method = "getInputStream", at = @At("RETURN"), cancellable = true, require = 1)
    private void nova$identifyModelJsonSource(final ResourceLocation location,
                                              final IResourcePack resourcePack,
                                              final CallbackInfoReturnable<InputStream> cir) {
        final ModelJsonMetadataIndex metadata = ModelJsonMetadataIndexImpl.instance();
        if (!metadata.isActive() || !location.getPath().endsWith(".json")) {
            return;
        }

        final boolean cacheable = resourcePack instanceof FolderResourcePack
            || resourcePack instanceof FileResourcePack
            || resourcePack instanceof DefaultResourcePack
            || resourcePack instanceof StellarCoreResourcePack stellarPack
            && !stellarPack.stellar_core$isMutableResourcePack();
        cir.setReturnValue(new SourcedResourceInputStream(cir.getReturnValue(), resourcePack, cacheable));
    }
}
