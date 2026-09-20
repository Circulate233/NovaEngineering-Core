package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.texture.TextureGeneration;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Implements texture lifetime tracking without relying on reusable OpenGL texture identifiers.
 */
@Mixin(AbstractTexture.class)
public abstract class MixinAbstractTexture implements TextureGeneration {

    @Unique
    private long nova$deleteGeneration;

    @Inject(method = "deleteGlTexture", at = @At("RETURN"), require = 1)
    private void nova$advanceDeleteGeneration(final CallbackInfo ci) {
        this.nova$deleteGeneration++;
    }

    /**
     * Supplies the generation used by texture-specific upload caches.
     *
     * @return the number of completed deletion requests for this texture
     */
    @Override
    public long nova$getDeleteGeneration() {
        return this.nova$deleteGeneration;
    }
}
