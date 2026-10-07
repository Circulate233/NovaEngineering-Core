package github.kasuminova.novaeng.mixin.actinium;

import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.memory.FallbackScratchAccess;
import github.kasuminova.novaeng.client.memory.ScratchRetention;
import net.minecraft.client.renderer.BufferBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** Trims the legacy vanilla BufferBuilders retained by Actinium's Vintage build context. */
@Mixin(value = VintageChunkBuildContext.class, remap = false)
public abstract class MixinVintageScratchMemory implements FallbackScratchAccess {
    @Shadow @Final private BufferBuilder[] worldRenderers;
    @Shadow @Final private boolean[] usedWorldRenderers;
    @Unique private final ScratchRetention[] nova$retention =
        new ScratchRetention[VintageChunkBuildContext.LAYERS.length];
    @Unique private final int[] nova$usedBytes = new int[VintageChunkBuildContext.LAYERS.length];

    @Redirect(method = "convertVanillaDataToCeleritasData", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/BufferBuilder;getByteBuffer()Ljava/nio/ByteBuffer;", remap = true), require = 1)
    private ByteBuffer nova$recordFallbackUse(final BufferBuilder builder) {
        final ByteBuffer bytes = builder.getByteBuffer();
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
        for (int i = 0; i < worldRenderers.length; i++) {
            final BufferBuilder builder = worldRenderers[i];
            if (builder == null) { continue; }
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
        Arrays.fill(worldRenderers, null);
        Arrays.fill(usedWorldRenderers, false);
        Arrays.fill(nova$usedBytes, 0);
    }
}
