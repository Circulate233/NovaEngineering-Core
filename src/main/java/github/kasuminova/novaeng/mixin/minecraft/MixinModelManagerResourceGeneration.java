package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.model.CacheCapacityProbe;
import github.kasuminova.novaeng.client.model.MultipartModelCache;
import github.kasuminova.novaeng.client.model.PredicateCache;
import github.kasuminova.novaeng.client.model.TextureLocationCache;
import github.kasuminova.novaeng.client.model.async.ModelBookshelfBakeCache;
import github.kasuminova.novaeng.client.resource.DirectoryExistenceCache;
import github.kasuminova.novaeng.client.resource.ModelJsonMetadataIndex;
import github.kasuminova.novaeng.client.resource.ModelJsonMetadataIndexImpl;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/**
 * Bounds reload-scoped state across normal and nested model reloads without wrapping the method.
 */
@Mixin(ModelManager.class)
public abstract class MixinModelManagerResourceGeneration {
    @Unique
    private static final ThreadLocal<int[]> nova$generationScopes = ThreadLocal.withInitial(() -> new int[4]);

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"), require = 1)
    private void nova$beginResourceGeneration(final IResourceManager resourceManager, final CallbackInfo ci) {
        ModelBookshelfBakeCache.clear();
        PredicateCache.clear();
        MultipartModelCache.clear();
        final boolean metadata = NovaEngCoreConfig.CLIENT.optimizeModelJsonMetadata;
        final boolean existence = NovaEngCoreConfig.CLIENT.optimizeResourceExistence;
        final ModelJsonMetadataIndex metadataIndex = metadata ? ModelJsonMetadataIndexImpl.instance() : null;
        final DirectoryExistenceCache existenceCache = existence ? DirectoryExistenceCache.instance() : null;
        if (metadataIndex != null) {
            metadataIndex.begin();
        }
        if (existenceCache != null) {
            existenceCache.begin();
        }
        final boolean textureLocations = NovaEngCoreConfig.CLIENT.optimizeModelBakeInputs;
        if (textureLocations) {
            TextureLocationCache.begin();
        }
        int flags = (metadataIndex == null ? 0 : 1) | (existenceCache == null ? 0 : 2);
        if (textureLocations) {
            flags |= 4;
        }
        int[] scopes = nova$generationScopes.get();
        int depth = scopes[0];
        if (depth + 1 >= scopes.length) {
            scopes = Arrays.copyOf(scopes, scopes.length << 1);
            nova$generationScopes.set(scopes);
        }
        scopes[++depth] = flags;
        scopes[0] = depth;
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"), require = 1)
    private void nova$endResourceGeneration(final IResourceManager resourceManager, final CallbackInfo ci) {
        final int[] scopes = nova$generationScopes.get();
        final int depth = scopes[0];
        if (depth == 0) {
            return;
        }
        final int flags = scopes[depth];
        scopes[depth] = 0;
        scopes[0] = depth - 1;

        if ((flags & 4) != 0) {
            TextureLocationCache.end();
        }

        CacheCapacityProbe.report();
        MultipartModelCache.clear();
        try {
            if ((flags & 2) != 0) {
                DirectoryExistenceCache.instance().end();
            }
        } finally {
            if ((flags & 1) != 0) {
                ModelJsonMetadataIndexImpl.instance().end();
            }
        }
    }
}
