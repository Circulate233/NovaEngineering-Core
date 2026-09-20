package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.resource.DirectoryExistenceCache;
import github.kasuminova.novaeng.client.resource.ModelJsonMetadataIndex;
import github.kasuminova.novaeng.client.resource.ModelJsonMetadataIndexImpl;
import github.kasuminova.novaeng.client.model.async.ModelBookshelfBakeCache;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Bounds reload scoped resource state to one complete model reload, including exceptional exits.
 */
@Mixin(ModelManager.class)
public abstract class MixinModelManagerResourceGeneration {

    @WrapMethod(method = "onResourceManagerReload", require = 1)
    private void nova$withResourceGeneration(final IResourceManager resourceManager,
                                             final Operation<Void> original) {
        ModelBookshelfBakeCache.clear();
        final boolean metadata = NovaEngCoreConfig.CLIENT.optimizeModelJsonMetadata;
        final boolean existence = NovaEngCoreConfig.CLIENT.optimizeResourceExistence;
        if (!metadata && !existence) {
            original.call(resourceManager);
            return;
        }

        final ModelJsonMetadataIndex metadataIndex = metadata ? ModelJsonMetadataIndexImpl.instance() : null;
        final DirectoryExistenceCache existenceCache = existence ? DirectoryExistenceCache.instance() : null;
        if (metadataIndex != null) {
            metadataIndex.begin();
        }
        if (existenceCache != null) {
            existenceCache.begin();
        }
        try {
            original.call(resourceManager);
        } finally {
            try {
                if (existenceCache != null) {
                    existenceCache.end();
                }
            } finally {
                if (metadataIndex != null) {
                    metadataIndex.end();
                }
            }
        }
    }
}
