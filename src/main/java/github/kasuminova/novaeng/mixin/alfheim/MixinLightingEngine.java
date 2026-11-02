package github.kasuminova.novaeng.mixin.alfheim;

import dev.redstudio.alfheim.lighting.LightingEngine;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.util.ClientLightingGuard;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LightingEngine.class, remap = false)
public abstract class MixinLightingEngine {

    @Unique
    private final boolean[] nova$chunkCacheValid = new boolean[8];
    @Unique
    private final long[] nova$chunkCacheKeys = new long[8];
    @Unique
    private final Chunk[] nova$chunkCacheValues = new Chunk[8];

    @Shadow
    @Final
    private World world;

    @Inject(method = "scheduleLightUpdate(Lnet/minecraft/world/EnumSkyBlock;Lnet/minecraft/util/math/BlockPos;)V",
        at = @At("HEAD"), cancellable = true)
    private void nova$skipScheduleLightUpdate(final CallbackInfo ci) {
        if (!ClientLightingGuard.shouldProcessLighting(this.world)) {
            ci.cancel();
        }
    }

    @Inject(method = "processLightUpdatesForTypeInner(Lnet/minecraft/world/EnumSkyBlock;Ldev/redstudio/alfheim/utils/DeduplicatedLongQueue;)V",
        at = @At("HEAD"), require = 1)
    private void nova$resetChunkLookup(final CallbackInfo ci) {
        for (int index = 0; index < this.nova$chunkCacheValid.length; index++) {
            this.nova$chunkCacheValid[index] = false;
            this.nova$chunkCacheValues[index] = null;
        }
    }

    @Redirect(method = "getChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/Chunk;",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/IChunkProvider;getLoadedChunk(II)Lnet/minecraft/world/chunk/Chunk;", remap = true),
        require = 1)
    private Chunk nova$reuseLoadedChunk(final IChunkProvider provider, final int chunkX, final int chunkZ) {
        if (!NovaEngCoreConfig.SERVER.optimizeAlfheimChunkLookup) {
            return provider.getLoadedChunk(chunkX, chunkZ);
        }
        final long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
        final int slot = (chunkX * 31 + chunkZ) & (this.nova$chunkCacheValid.length - 1);
        if (this.nova$chunkCacheValid[slot] && this.nova$chunkCacheKeys[slot] == key) {
            return this.nova$chunkCacheValues[slot];
        }
        final Chunk chunk = provider.getLoadedChunk(chunkX, chunkZ);
        this.nova$chunkCacheValid[slot] = true;
        this.nova$chunkCacheKeys[slot] = key;
        this.nova$chunkCacheValues[slot] = chunk;
        return chunk;
    }
}
