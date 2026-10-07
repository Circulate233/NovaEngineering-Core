package github.kasuminova.novaeng.mixin.actinium;

import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import dhj.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildContext;
import github.kasuminova.novaeng.client.memory.FallbackScratchAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Runs cleanup for the Vintage fallback buffers around Actinium's shared build context. */
@Mixin(value = ChunkBuildContext.class, remap = false)
public abstract class MixinBuildContextMemory {
    @Redirect(method = "cleanup", at = @At(value = "INVOKE",
        target = "Ldhj/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;resetForTask()V"), require = 1)
    private void nova$finishContext(final ChunkBuildBuffers buffers) {
        buffers.resetForTask();
        if ((Object) this instanceof FallbackScratchAccess fallback) {
            fallback.nova$finishFallbackTask();
        }
    }

    @Redirect(method = "destroy", at = @At(value = "INVOKE",
        target = "Ldhj/embeddedt/embeddium/impl/render/chunk/compile/ChunkBuildBuffers;destroy()V"), require = 1)
    private void nova$destroyContext(final ChunkBuildBuffers buffers) {
        try {
            buffers.destroy();
        } finally {
            if ((Object) this instanceof FallbackScratchAccess fallback) {
                fallback.nova$releaseFallback();
            }
        }
    }
}
