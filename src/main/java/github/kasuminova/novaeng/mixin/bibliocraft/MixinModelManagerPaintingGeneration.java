package github.kasuminova.novaeng.mixin.bibliocraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.kasuminova.novaeng.client.resource.BiblioCraftPaintingCache;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.resources.IResourceManager;
import org.spongepowered.asm.mixin.Mixin;

/** Bounds the optional BiblioCraft scan cache to one complete model resource reload. */
@Mixin(ModelManager.class)
public abstract class MixinModelManagerPaintingGeneration {

    @WrapMethod(
        method = "onResourceManagerReload",
        require = 1)
    private void nova$withBiblioCraftPaintingGeneration(final IResourceManager resourceManager,
                                                        final Operation<Void> original) {
        BiblioCraftPaintingCache.beginGeneration();
        try {
            original.call(resourceManager);
        } finally {
            BiblioCraftPaintingCache.endGeneration();
        }
    }
}
