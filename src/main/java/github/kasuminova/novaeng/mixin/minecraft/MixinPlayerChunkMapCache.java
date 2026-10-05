package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.common.network.bandwidth.DelayedChunkCache;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Mixin(PlayerChunkMap.class)
public abstract class MixinPlayerChunkMapCache {
    @Final
    @Shadow
    private List<EntityPlayerMP> players;

    @Shadow
    private int playerViewRadius;

    @Shadow
    public abstract PlayerChunkMapEntry getEntry(int x, int z);

    @Unique
    private final Reference2ObjectMap<EntityPlayerMP, DelayedChunkCache> novaeng$chunkCaches = new Reference2ObjectOpenHashMap<>();

    @Redirect(method = "updateMovingPlayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/management/PlayerChunkMapEntry;addPlayer(Lnet/minecraft/entity/player/EntityPlayerMP;)V"))
    private void novaeng$restoreCachedChunk(PlayerChunkMapEntry entry, EntityPlayerMP player) {
        DelayedChunkCache cache = novaeng$chunkCaches.get(player);
        ChunkPos pos = entry.getPos();
        if (cache != null && cache.restore(pos.x, pos.z)) {
            if (entry.getChunk() != null && entry.isSentToPlayers()) {
                player.getServerWorld().getEntityTracker().sendLeashedEntitiesInChunk(player, entry.getChunk());
            }
        } else {
            entry.addPlayer(player);
        }
    }

    @Redirect(method = "updateMovingPlayer", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/management/PlayerChunkMapEntry;removePlayer(Lnet/minecraft/entity/player/EntityPlayerMP;)V"))
    private void novaeng$retainLeavingChunk(PlayerChunkMapEntry entry, EntityPlayerMP player) {
        var config = NovaEngCoreConfig.NETWORK;
        if (!config.delayedChunkCache || config.delayedChunkCacheSize <= 0 || config.delayedChunkCacheDistance <= 0
            || !entry.isSentToPlayers() || entry.getChunk() == null) {
            entry.removePlayer(player);
            return;
        }
        ChunkPos pos = entry.getPos();
        int centerX = MathHelper.floor(player.posX) >> 4;
        int centerZ = MathHelper.floor(player.posZ) >> 4;
        DelayedChunkCache cache = novaeng$chunkCaches.computeIfAbsent(player, ignored -> new DelayedChunkCache());
        if (!cache.retain(pos.x, pos.z, centerX, centerZ, playerViewRadius,
            config.delayedChunkCacheDistance, config.delayedChunkCacheSize, System.nanoTime(),
            TimeUnit.SECONDS.toNanos(config.delayedChunkCacheSeconds), key -> novaeng$evict(player, key))) {
            entry.removePlayer(player);
        }
    }

    @Inject(method = "isPlayerWatchingChunk", at = @At("HEAD"), cancellable = true)
    private void novaeng$ignoreRetainedChunksForEntityTracking(EntityPlayerMP player, int chunkX, int chunkZ,
                                                               CallbackInfoReturnable<Boolean> result) {
        DelayedChunkCache cache = novaeng$chunkCaches.get(player);
        if (cache != null && cache.contains(chunkX, chunkZ)) {
            result.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void novaeng$expireRetainedChunks(CallbackInfo ci) {
        var config = NovaEngCoreConfig.NETWORK;
        long now = System.nanoTime();
        for (EntityPlayerMP player : players) {
            DelayedChunkCache cache = novaeng$chunkCaches.get(player);
            if (cache == null) {
                continue;
            }
            cache.expire(MathHelper.floor(player.posX) >> 4, MathHelper.floor(player.posZ) >> 4, playerViewRadius,
                config.delayedChunkCacheDistance, config.delayedChunkCacheSize,
                now, TimeUnit.SECONDS.toNanos(config.delayedChunkCacheSeconds), key -> novaeng$evict(player, key));
            if (cache.isEmpty()) {
                novaeng$chunkCaches.remove(player);
            }
        }
    }

    @Inject(method = "removePlayer", at = @At("HEAD"))
    private void novaeng$clearOnLeave(EntityPlayerMP player, CallbackInfo ci) {
        DelayedChunkCache cache = novaeng$chunkCaches.remove(player);
        if (cache != null) {
            cache.clear(key -> novaeng$evict(player, key));
        }
    }

    @Inject(method = "setPlayerViewRadius", at = @At("HEAD"))
    private void novaeng$clearOnRadiusChange(int radius, CallbackInfo ci) {
        if (radius == playerViewRadius) {
            return;
        }
        for (var entry : novaeng$chunkCaches.entrySet()) {
            EntityPlayerMP player = entry.getKey();
            entry.getValue().clear(key -> novaeng$evict(player, key));
        }
        novaeng$chunkCaches.clear();
    }

    @Unique
    private void novaeng$evict(EntityPlayerMP player, long key) {
        PlayerChunkMapEntry entry = getEntry(DelayedChunkCache.x(key), DelayedChunkCache.z(key));
        if (entry != null) {
            entry.removePlayer(player);
        }
    }
}
