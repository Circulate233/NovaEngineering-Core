package github.kasuminova.novaeng.mixin.actinium;

import github.kasuminova.novaeng.client.entity.EntityChunkState;
import github.kasuminova.novaeng.client.entity.LoadedEntityChunkIndex;
import github.kasuminova.novaeng.client.entity.LoadedEntityChunkIndexProvider;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the per-world entity chunk index aligned with client chunk load and unload operations.
 */
@Mixin(ChunkProviderClient.class)
public abstract class MixinChunkProviderClient {

    @Shadow
    @Final
    private World world;

    @Inject(method = "loadChunk", at = @At("RETURN"), require = 1)
    private void nova$indexLoadedEntityChunk(final int chunkX,
                                             final int chunkZ,
                                             final CallbackInfoReturnable<Chunk> cir) {
        final Chunk chunk = cir.getReturnValue();
        if (chunk instanceof EntityChunkState state && state.nova$hasEntities()) {
            final LoadedEntityChunkIndex index = this.nova$getIndex();
            if (index != null) {
                index.put(chunk);
            }
        }
    }

    @Inject(method = "unloadChunk", at = @At("RETURN"), require = 1)
    private void nova$removeUnloadedEntityChunk(final int x,
                                                final int z,
                                                final CallbackInfo ci) {
        final LoadedEntityChunkIndex index = this.nova$getIndex();
        if (index != null) {
            index.remove(x, z);
        }
    }

    @Unique
    private LoadedEntityChunkIndex nova$getIndex() {
        return this.world instanceof LoadedEntityChunkIndexProvider provider
            ? provider.nova$getLoadedEntityChunkIndex()
            : null;
    }
}
