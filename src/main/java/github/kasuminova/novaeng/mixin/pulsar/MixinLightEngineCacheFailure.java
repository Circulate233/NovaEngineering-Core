package github.kasuminova.novaeng.mixin.pulsar;

import com.sumirelabs.pulsar.api.ExtendedWorld;
import com.sumirelabs.pulsar.light.SWMRNibbleArray;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "com.sumirelabs.pulsar.light.engine.LightEngineCache", remap = false)
public abstract class MixinLightEngineCacheFailure {
    @Shadow
    @Final
    protected World world;

    @Shadow
    protected abstract void resetTaskState();

    @Shadow
    protected abstract void setupEncodeOffset(int x, int y, int z);

    @Shadow
    protected abstract boolean canUseChunk(Chunk chunk);

    @Shadow
    protected abstract boolean[] getEmptinessMap(Chunk chunk);

    @Shadow
    protected abstract SWMRNibbleArray[] getNibblesOnChunk(Chunk chunk);

    @Shadow
    protected abstract void setChunkInCache(int x, int z, Chunk chunk);

    @Shadow
    protected abstract void setEmptinessMapCache(int x, int z, boolean[] map);

    @Shadow
    protected abstract void setBlocksForChunkInCache(int x, int z, ExtendedBlockStorage[] sections);

    @Shadow
    protected abstract void setNibblesForChunkInCache(int x, int z, SWMRNibbleArray[] nibbles);

    @Shadow
    protected abstract void destroyCaches();

    /**
     * @author NovaEngineering
     * @reason Apply Pulsar ab299af's cache cleanup in the declaring method, before any caller can reuse the engine.
     */
    @Overwrite
    protected final void setupCaches(final int x, final int y, final int z, final boolean relaxed, final boolean extended) {
        try {
            this.resetTaskState();
            final int centerX = x >> 4;
            final int centerZ = z >> 4;
            this.setupEncodeOffset(centerX * 16 + 7, y, centerZ * 16 + 7);

            final ExtendedWorld chunks = (ExtendedWorld) this.world;
            final int radius = extended ? 2 : 1;
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    final int chunkX = centerX + dx;
                    final int chunkZ = centerZ + dz;
                    final boolean outerRing = dx == -2 || dx == 2 || dz == -2 || dz == 2;
                    final Chunk chunk = chunks.pulsar$getAnyChunkImmediately(chunkX, chunkZ);
                    if (chunk == null) {
                        if (!relaxed && !outerRing) {
                            throw new IllegalArgumentException("Trying to propagate light update before 1 radius neighbours ready");
                        }
                        continue;
                    }
                    if (!this.canUseChunk(chunk)) continue;

                    this.setChunkInCache(chunkX, chunkZ, chunk);
                    this.setEmptinessMapCache(chunkX, chunkZ, this.getEmptinessMap(chunk));
                    if (!outerRing) {
                        this.setBlocksForChunkInCache(chunkX, chunkZ, chunk.getBlockStorageArray());
                        this.setNibblesForChunkInCache(chunkX, chunkZ, this.getNibblesOnChunk(chunk));
                    }
                }
            }
        } catch (final Throwable failure) {
            this.destroyCaches();
            throw failure;
        }
    }
}
