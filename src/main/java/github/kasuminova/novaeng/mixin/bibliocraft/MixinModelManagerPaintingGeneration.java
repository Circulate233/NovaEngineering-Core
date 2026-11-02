package github.kasuminova.novaeng.mixin.bibliocraft;

import github.kasuminova.novaeng.client.resource.BiblioCraftPaintingCache;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bounds the optional BiblioCraft scan cache to one complete model resource reload. */
@Mixin(ModelManager.class)
public abstract class MixinModelManagerPaintingGeneration {

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"), require = 1)
    private void nova$beginBiblioCraftPaintingGeneration(final IResourceManager resourceManager,
                                                         final CallbackInfo ci) {
        BiblioCraftPaintingCache.beginGeneration();
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"), require = 1)
    private void nova$endBiblioCraftPaintingGeneration(final IResourceManager resourceManager,
                                                       final CallbackInfo ci) {
        BiblioCraftPaintingCache.endGeneration();
    }
}
