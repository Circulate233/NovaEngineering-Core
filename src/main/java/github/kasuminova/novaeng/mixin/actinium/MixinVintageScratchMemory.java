package github.kasuminova.novaeng.mixin.actinium;

import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.memory.FallbackScratchAccess;
import github.kasuminova.novaeng.client.memory.ScratchRetention;
import github.kasuminova.novaeng.common.util.VintageRendererAccess;
import net.minecraft.client.renderer.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Trims the legacy vanilla BufferBuilders retained by Actinium's Vintage build context.
 *
 * <p>The buffer array is reached through {@link VintageRendererAccess} instead of a {@code @Shadow} field: its
 * declared element type changed between Actinium versions and a shadow is matched by name and descriptor, so one
 * hard-coded type would stop this mixin from applying to the other version.
 */
@Mixin(value = VintageChunkBuildContext.class, remap = false)
public abstract class MixinVintageScratchMemory implements FallbackScratchAccess {
    @Unique
    private final ScratchRetention[] nova$retention =
        new ScratchRetention[VintageChunkBuildContext.LAYERS.length];
    @Unique
    private final int[] nova$usedBytes = new int[VintageChunkBuildContext.LAYERS.length];
    /**
     * Actinium keeps these array objects for the lifetime of the build context. Cache them after the first access so
     * the hot fallback paths do not repeatedly resolve the fields through {@link VintageRendererAccess}.
     */
    @Unique
    private BufferBuilder[] nova$worldRenderers;
    @Unique
    private boolean[] nova$usedWorldRenderers;

    @Redirect(method = "convertVanillaDataToCeleritasData", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/BufferBuilder;getByteBuffer()Ljava/nio/ByteBuffer;", remap = true), require = 1)
    private ByteBuffer nova$recordFallbackUse(final BufferBuilder builder) {
        final ByteBuffer bytes = builder.getByteBuffer();
        nova$cacheRendererArrays((VintageChunkBuildContext) (Object) this);
        final BufferBuilder[] worldRenderers = nova$worldRenderers;
        for (int i = 0; i < worldRenderers.length; i++) {
            if (worldRenderers[i] == builder) {
                nova$usedBytes[i] = Math.max(nova$usedBytes[i], bytes.remaining());
                break;
            }
        }
        return bytes;
    }

    @Override
    public void nova$finishFallbackTask() {
        final VintageChunkBuildContext context = (VintageChunkBuildContext) (Object) this;
        nova$cacheRendererArrays(context);
        final BufferBuilder[] worldRenderers = nova$worldRenderers;
        final boolean[] usedWorldRenderers = nova$usedWorldRenderers;
        for (int i = 0; i < worldRenderers.length; i++) {
            final BufferBuilder builder = worldRenderers[i];
            if (builder == null) {
                continue;
            }
            final int capacity = builder.getByteBuffer().capacity();
            // An exceptional build may still need Vintage.cleanup() to finishDrawing().
            final int used = usedWorldRenderers[i] ? capacity : nova$usedBytes[i];
            ScratchRetention retention = nova$retention[i];
            if (retention == null) {
                retention = nova$retention[i] = new ScratchRetention();
            }
            final boolean release = retention.endTask(used, capacity, 131072L * 4);
            if (NovaEngCoreConfig.PERFORMANCE.trimChunkBuildScratch && release && !usedWorldRenderers[i]) {
                worldRenderers[i] = null;
            }
            nova$usedBytes[i] = 0;
        }
    }

    @Override
    public void nova$releaseFallback() {
        nova$cacheRendererArrays((VintageChunkBuildContext) (Object) this);
        Arrays.fill(nova$worldRenderers, null);
        Arrays.fill(nova$usedWorldRenderers, false);
        Arrays.fill(nova$usedBytes, 0);
    }

    @Unique
    private void nova$cacheRendererArrays(final VintageChunkBuildContext context) {
        if (nova$worldRenderers != null) {
            return;
        }
        final BufferBuilder[] worldRenderers = VintageRendererAccess.renderers(context);
        nova$usedWorldRenderers = VintageRendererAccess.used(context);
        nova$worldRenderers = worldRenderers;
    }
}
