package github.kasuminova.novaeng.mixin.pulsar;

import com.llamalad7.mixinextras.sugar.Local;
import com.sumirelabs.pulsar.light.ChunkTasks;
import com.sumirelabs.pulsar.light.LightQueue;
import com.sumirelabs.pulsar.light.PulsarChunk;
import com.sumirelabs.pulsar.util.CoordinateUtils;
import com.sumirelabs.pulsar.util.SnapshotChunkMap;
import github.kasuminova.novaeng.common.performance.PulsarFailureAccess;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CancellationException;

// SCALAR failure handling from Pulsar 40828e6, adapted to the 0.6.2 generation coordinator.
@Mixin(targets = "com.sumirelabs.pulsar.light.InitialLightCoordinator", remap = false)
public abstract class MixinInitialLightCoordinator implements PulsarFailureAccess.Coordinator {
    @Shadow
    @Final
    private static int MAX_RELIGHT_ATTEMPTS;
    @Shadow
    @Final
    private SnapshotChunkMap loadedChunks;
    @Shadow
    @Final
    private LightQueue skyQueue;
    @Shadow
    @Final
    private LightQueue blockQueue;
    @Shadow
    @Final
    private Object lock;
    @Shadow
    @Final
    private Long2ObjectOpenHashMap<PulsarFailureAccess.Completion> completions;
    @Unique
    private volatile LongOpenHashSet novaeng$failedChunks;

    @Redirect(method = "lambda$queue$0", at = @At(value = "FIELD", target = "Lcom/sumirelabs/pulsar/light/ChunkLightCompletion;published:Z"), require = 1)
    private static boolean novaeng$forwardRecovery(@Coerce final Object recovery, @Coerce final Object superseded) {
        // The extra coerced argument is the first lambda parameter. ChunkLightCompletion is package-private.
        return ((PulsarFailureAccess.Completion) recovery).novaeng$forwardRecovery((PulsarFailureAccess.Completion) superseded);
    }

    @Shadow
    abstract boolean restartAfterEdgeOverflow(ChunkTasks task, int chunkX, int chunkZ, String engineName);

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "()Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;"), require = 1)
    private Long2ObjectOpenHashMap<PulsarFailureAccess.Completion> novaeng$bindCoordinator(
        @Local(argsOnly = true) final SnapshotChunkMap chunks) {
        // The manager constructs this coordinator before starting either worker.
        ((PulsarFailureAccess.ChunkMap) (Object) chunks).novaeng$bindLighting(this);
        return new Long2ObjectOpenHashMap<>();
    }

    @Override
    public final void novaeng$failTask(final ChunkTasks task, final Throwable failure) {
        ((PulsarFailureAccess.Task) (Object) task).novaeng$setFailure(failure);
        final long generation = task.initialLightChunk != null ? task.initialLightGeneration : task.initialLightEdgeGeneration;
        final long key = task.chunkCoordinate;
        final PulsarFailureAccess.Completion current;
        synchronized (this.lock) {
            current = this.completions.get(key);
            if (generation > 0L ? current == null || current.novaeng$generation() != generation : current != null) {
                return;
            }
            if (current != null) this.completions.remove(key);
            final Chunk chunk = this.loadedChunks.get(key);
            if (chunk != null && (current == null || current.novaeng$chunk() == chunk)) {
                final PulsarChunk light = (PulsarChunk) chunk;
                this.novaeng$markFailed(key, light);
                light.pulsar$setLightReady(false);
                light.pulsar$setLightUsable(false);
            }
        }
        if (current != null) current.novaeng$fail(failure);
    }

    // Both mutation helpers are called while holding the existing generation monitor.
    @Unique
    private void novaeng$markFailed(final long key, final PulsarChunk chunk) {
        LongOpenHashSet failed = this.novaeng$failedChunks;
        if (failed == null) {
            failed = new LongOpenHashSet(1);
            failed.add(key);
            this.novaeng$failedChunks = failed;
        } else {
            failed.add(key);
        }
        chunk.pulsar$setSavedLightValid(false);
    }

    @Unique
    private void novaeng$clearFailure(final long key) {
        final LongOpenHashSet failed = this.novaeng$failedChunks;
        if (failed != null && failed.remove(key) && failed.isEmpty()) this.novaeng$failedChunks = null;
    }

    @Override
    public final boolean novaeng$hasFailedNear(final int cx, final int cz) {
        if (this.novaeng$failedChunks == null) return false;
        synchronized (this.lock) {
            final LongOpenHashSet failed = this.novaeng$failedChunks;
            if (failed == null) return false;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (failed.contains(CoordinateUtils.getChunkKey(cx + dx, cz + dz))) return true;
                }
            }
            return false;
        }
    }

    @Redirect(method = "queue(IILnet/minecraft/world/chunk/Chunk;[Ljava/lang/Boolean;IZZ)Lcom/sumirelabs/pulsar/light/ChunkLightCompletion;",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;put(JLjava/lang/Object;)Ljava/lang/Object;"),
        require = 1)
    private Object novaeng$replaceGeneration(final Long2ObjectOpenHashMap<Object> map, final long key, final Object completion) {
        final Object previous = map.put(key, completion);
        this.novaeng$clearFailure(key);
        return previous;
    }

    /**
     * @author NovaEngineering
     * @reason Retire the generation, both lanes' work, and the per-coordinate failure marker together.
     */
    @Overwrite
    void removeChunk(final int cx, final int cz) {
        final long key = CoordinateUtils.getChunkKey(cx, cz);
        final PulsarFailureAccess.Completion removed;
        synchronized (this.lock) {
            removed = this.completions.remove(key);
            this.novaeng$clearFailure(key);
            if (this.skyQueue != null) this.skyQueue.removeChunk(cx, cz);
            if (this.blockQueue != null) this.blockQueue.removeChunk(cx, cz);
        }
        if (removed != null) removed.novaeng$finish(false);
    }

    @Override
    public final boolean novaeng$retryEdges(final ChunkTasks task, final int cx, final int cz, final String engineName) {
        if (task.initialLightEdgeGeneration <= 0L) return false;
        if (this.restartAfterEdgeOverflow(task, cx, cz, engineName)) return true;
        final Throwable failure = task.edgeCheckAttempts >= MAX_RELIGHT_ATTEMPTS
            ? new IllegalStateException("Final light edge reconciliation exhausted its retries")
            : new CancellationException("Chunk unloaded during edge retry");
        this.novaeng$failTask(task, failure);
        return false;
    }

    @Redirect(method = "completeInitial", at = @At(value = "INVOKE", ordinal = 1,
        target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;remove(J)Ljava/lang/Object;"), require = 1)
    private Object novaeng$recordFailedTransition(final Long2ObjectOpenHashMap<PulsarFailureAccess.Completion> map,
                                                  final long key, @Local(argsOnly = true) final ChunkTasks task,
                                                  @Local(name = "t") final Throwable failure) {
        final PulsarFailureAccess.Completion removed = map.remove(key);
        this.novaeng$markFailed(key, (PulsarChunk) removed.novaeng$chunk());
        ((PulsarFailureAccess.Task) (Object) task).novaeng$setFailure(failure);
        return removed;
    }

    @Redirect(method = "completeEdges", at = @At(value = "INVOKE",
        target = "Lcom/sumirelabs/pulsar/light/PulsarChunk;pulsar$syncLightToVanilla()V"), require = 1)
    private void novaeng$copyCompletedLight(final PulsarChunk chunk, @Local(argsOnly = true) final ChunkTasks task) {
        try {
            chunk.pulsar$syncLightToVanilla();
        } catch (final Throwable failure) {
            ((PulsarFailureAccess.Task) (Object) task).novaeng$setFailure(failure);
            throw failure;
        }
    }

    @Redirect(method = "completeEdges", at = @At(value = "INVOKE",
        target = "Lcom/sumirelabs/pulsar/light/PulsarChunk;pulsar$setLightReady(Z)V"), require = 3, allow = 3)
    private void novaeng$publishCompletedLight(final PulsarChunk chunk, final boolean ready,
                                               @Local(argsOnly = true) final ChunkTasks task) {
        if (!ready) this.novaeng$markFailed(task.chunkCoordinate, chunk);
        try {
            chunk.pulsar$setLightReady(ready);
        } catch (final Throwable failure) {
            final PulsarFailureAccess.Task result = (PulsarFailureAccess.Task) (Object) task;
            if (result.novaeng$failure() == null) result.novaeng$setFailure(failure);
            throw failure;
        }
    }

    @Redirect(method = {"completeInitial", "completeEdges"}, at = @At(value = "INVOKE",
        target = "Lcom/sumirelabs/pulsar/light/ChunkLightCompletion;finish(Z)V"), require = 2, allow = 2)
    private void novaeng$finishGeneration(@Coerce final Object completion, final boolean published,
                                          @Local(argsOnly = true) final ChunkTasks task) {
        final PulsarFailureAccess.Completion handle = (PulsarFailureAccess.Completion) completion;
        final Throwable failure = ((PulsarFailureAccess.Task) (Object) task).novaeng$failure();
        if (failure == null) handle.novaeng$finish(published);
        else handle.novaeng$fail(failure);
    }
}
