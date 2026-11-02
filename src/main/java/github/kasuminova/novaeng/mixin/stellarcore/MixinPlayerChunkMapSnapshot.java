package github.kasuminova.novaeng.mixin.stellarcore;

import com.google.common.base.Predicate;
import github.kasuminova.novaeng.common.performance.TickSnapshotPool;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

@Mixin(PlayerChunkMap.class)
public abstract class MixinPlayerChunkMapSnapshot {

    @Unique
    private static final Function<PlayerChunkMapEntry, Chunk> nova$tickableChunk = entry -> {
        final Chunk chunk = entry.getChunk();
        return chunk != null && ((!chunk.isLightPopulated() && chunk.isTerrainPopulated())
            || !chunk.wasTicked() || entry.hasPlayerMatchingInRange(128.0, MixinPlayerChunkMapSnapshot.NOT_SPECTATOR))
            ? chunk : null;
    };
    @Shadow
    @Final
    private static Predicate<EntityPlayerMP> NOT_SPECTATOR;
    @Shadow
    @Final
    private List<PlayerChunkMapEntry> entries;
    @Unique
    private TickSnapshotPool<Chunk> nova$chunkSnapshots;

    @Inject(method = "<init>(Lnet/minecraft/world/WorldServer;)V", at = @At("RETURN"), require = 1)
    private void nova$initializeSnapshotPool(final WorldServer serverWorld, final CallbackInfo ci) {
        this.nova$chunkSnapshots = new TickSnapshotPool<>();
    }

    @Inject(method = "getChunkIterator()Ljava/util/Iterator;", at = @At("HEAD"), cancellable = true, require = 1)
    private void nova$chunkTickSnapshot(final CallbackInfoReturnable<Iterator<Chunk>> cir) {
        cir.setReturnValue(this.nova$chunkSnapshots.selectNonNull(this.entries, nova$tickableChunk));
    }
}
