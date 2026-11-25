package github.kasuminova.novaeng.mixin.pulsar;

import com.llamalad7.mixinextras.sugar.Local;
import com.sumirelabs.pulsar.light.ChunkTasks;
import com.sumirelabs.pulsar.light.ContextualLightManager;
import com.sumirelabs.pulsar.light.LightQueue;
import com.sumirelabs.pulsar.light.PulsarChunk;
import com.sumirelabs.pulsar.light.WorldLightManager;
import com.sumirelabs.pulsar.light.engine.PulsarEngine;
import com.sumirelabs.pulsar.rgb.RgbContextualLightManager;
import com.sumirelabs.pulsar.util.SnapshotChunkMap;
import github.kasuminova.novaeng.common.performance.PulsarFailureAccess;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CancellationException;

// Vanilla/Forge light readiness and failure handling from Pulsar 40828e6.
@Mixin(value = WorldLightManager.class, remap = false)
public abstract class MixinWorldLightManager {
    @Shadow
    @Final
    private SnapshotChunkMap loadedChunkMap;
    @Shadow
    @Final
    private ContextualLightManager contextualLight;
    @Shadow
    private volatile RgbContextualLightManager rgbContextualLight;

    @Shadow
    public abstract Chunk getLoadedChunk(int chunkX, int chunkZ);

    @Shadow
    public abstract boolean hasPendingLightWorkNear(int chunkX, int chunkZ);

    @Shadow
    private void processSkyTask(final ChunkTasks task, final PulsarEngine engine) {
        throw new AssertionError();
    }

    @Shadow
    private void processBlockTask(final ChunkTasks task, final PulsarEngine engine) {
        throw new AssertionError();
    }

    @Shadow
    private boolean finishUnloadWait(final long waitedNs, final boolean settled) {
        throw new AssertionError();
    }

    @Shadow
    private boolean requeueAfterOverflow(final LightQueue queue, final ChunkTasks task, final int cx, final int cz, final String engineName) {
        throw new AssertionError();
    }

    @Unique
    private PulsarFailureAccess.Coordinator novaeng$coordinator() {
        return ((PulsarFailureAccess.ChunkMap) (Object) this.loadedChunkMap).novaeng$lighting();
    }

    @Redirect(method = {"processSkyTask", "processBlockTask"}, at = @At(value = "INVOKE",
        target = "Lcom/sumirelabs/pulsar/util/SnapshotChunkMap;get(J)Lnet/minecraft/world/chunk/Chunk;", ordinal = 0),
        require = 2, allow = 2)
    private Chunk novaeng$checkTaskChunk(final SnapshotChunkMap chunks, final long key, @Local(argsOnly = true) final ChunkTasks task) {
        final Chunk chunk = chunks.get(key);
        if (chunk == null)
            this.novaeng$coordinator().novaeng$failTask(task, new CancellationException("Chunk unloaded during lighting"));
        return chunk;
    }

    @Redirect(method = {"processSkyTask", "processBlockTask"}, at = @At(value = "INVOKE",
        target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"),
        require = 2, allow = 2)
    private void novaeng$rejectFailedTask(final Logger logger, final String message, final Object cx, final Object cz,
                                          final Object failure, @Local(argsOnly = true) final ChunkTasks task) {
        this.novaeng$coordinator().novaeng$failTask(task, (Throwable) failure);
        logger.error(message, cx, cz, failure);
    }

    @Redirect(method = {"processSkyTask", "processBlockTask"}, at = @At(value = "INVOKE",
        target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V"),
        require = 2, allow = 2)
    private void novaeng$rejectUnloadedTask(final Logger logger, final String message, final Object cx, final Object cz,
                                            final Object failure, @Local(argsOnly = true) final ChunkTasks task) {
        this.novaeng$coordinator().novaeng$failTask(task, (Throwable) failure);
        logger.warn(message, cx, cz, failure);
    }

    @Redirect(method = "processWithGate", at = @At(value = "INVOKE", target =
        "Lcom/sumirelabs/pulsar/light/WorldLightManager;processSkyTask(Lcom/sumirelabs/pulsar/light/ChunkTasks;Lcom/sumirelabs/pulsar/light/engine/PulsarEngine;)V"),
        require = 2, allow = 2)
    private void novaeng$guardSkyTask(final WorldLightManager manager, final ChunkTasks task, final PulsarEngine engine) {
        try {
            this.processSkyTask(task, engine);
        } catch (final Throwable failure) {
            this.novaeng$coordinator().novaeng$failTask(task, failure);
            throw failure;
        }
    }

    @Redirect(method = "processWithGate", at = @At(value = "INVOKE", target =
        "Lcom/sumirelabs/pulsar/light/WorldLightManager;processBlockTask(Lcom/sumirelabs/pulsar/light/ChunkTasks;Lcom/sumirelabs/pulsar/light/engine/PulsarEngine;)V"),
        require = 2, allow = 2)
    private void novaeng$guardBlockTask(final WorldLightManager manager, final ChunkTasks task, final PulsarEngine engine) {
        try {
            this.processBlockTask(task, engine);
        } catch (final Throwable failure) {
            this.novaeng$coordinator().novaeng$failTask(task, failure);
            throw failure;
        }
    }

    @Redirect(method = {"processSkyTask", "processBlockTask"}, at = @At(value = "INVOKE", target =
        "Lcom/sumirelabs/pulsar/light/InitialLightCoordinator;restartAfterEdgeOverflow(Lcom/sumirelabs/pulsar/light/ChunkTasks;IILjava/lang/String;)Z"),
        require = 2, allow = 2)
    private boolean novaeng$retryTaggedEdges(@Coerce final Object coordinator, final ChunkTasks task,
                                             final int cx, final int cz, final String engineName) {
        return ((PulsarFailureAccess.Coordinator) coordinator).novaeng$retryEdges(task, cx, cz, engineName);
    }

    @Redirect(method = {"processWithGate", "processSkyTask", "processBlockTask"}, at = @At(value = "INVOKE", target =
        "Lcom/sumirelabs/pulsar/light/WorldLightManager;requeueAfterOverflow(Lcom/sumirelabs/pulsar/light/LightQueue;Lcom/sumirelabs/pulsar/light/ChunkTasks;IILjava/lang/String;)Z"),
        require = 6, allow = 6)
    private boolean novaeng$checkOverflowRetry(final WorldLightManager manager, final LightQueue queue, final ChunkTasks task,
                                               final int cx, final int cz, final String engineName) {
        // Mixed edge-phase tasks consume the same bounded recovery budget as edge-only tasks.
        final PulsarFailureAccess.Coordinator coordinator = this.novaeng$coordinator();
        if (task.initialLightChunk == null && task.initialLightEdgeGeneration > 0L) {
            return coordinator.novaeng$retryEdges(task, cx, cz, engineName);
        }
        if (this.requeueAfterOverflow(queue, task, cx, cz, engineName)) return true;
        final Throwable failure = this.loadedChunkMap.get(task.chunkCoordinate) == null
            ? new CancellationException("Chunk unloaded during retry")
            : new IllegalStateException(engineName + " lighting exhausted its retries");
        coordinator.novaeng$failTask(task, failure);
        return false;
    }

    /**
     * @author NovaEngineering
     * @reason Prevent a failed propagation in the write neighborhood from reaching players.
     */
    @Overwrite
    public boolean canSendUpdatedChunkLight(final int cx, final int cz) {
        final Chunk chunk = this.getLoadedChunk(cx, cz);
        return chunk != null && ((PulsarChunk) chunk).pulsar$isLightReady()
            && !this.hasPendingLightWorkNear(cx, cz) && !this.novaeng$coordinator().novaeng$hasFailedNear(cx, cz);
    }

    /**
     * @author NovaEngineering
     * @reason Refuse to persist partial propagation as valid while preserving the existing contextual sample check.
     */
    @Overwrite
    public boolean hasFailedLightSamplesNear(final int cx, final int cz) {
        if (this.novaeng$coordinator().novaeng$hasFailedNear(cx, cz)) return true;
        final RgbContextualLightManager rgb = this.rgbContextualLight;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (this.contextualLight.hasFailedSamples(cx + dx, cz + dz)
                    || (rgb != null && rgb.hasFailedSamples(cx + dx, cz + dz))) return true;
            }
        }
        return false;
    }

    @Redirect(method = "awaitPendingWork", at = @At(value = "INVOKE",
        target = "Lcom/sumirelabs/pulsar/light/WorldLightManager;finishUnloadWait(JZ)Z", ordinal = 0), require = 1)
    private boolean novaeng$checkSettledNeighborhood(final WorldLightManager manager, final long waitedNs, final boolean settled,
                                                     @Local(argsOnly = true, ordinal = 0) final int cx,
                                                     @Local(argsOnly = true, ordinal = 1) final int cz) {
        return this.finishUnloadWait(waitedNs, settled && !this.novaeng$coordinator().novaeng$hasFailedNear(cx, cz));
    }
}
