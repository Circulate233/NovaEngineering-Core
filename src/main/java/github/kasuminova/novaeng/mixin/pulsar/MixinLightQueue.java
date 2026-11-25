package github.kasuminova.novaeng.mixin.pulsar;

import com.sumirelabs.pulsar.light.ChunkTasks;
import com.sumirelabs.pulsar.light.LightQueue;
import github.kasuminova.novaeng.common.performance.PulsarFailureAccess;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayDeque;

@Mixin(value = LightQueue.class, remap = false)
public abstract class MixinLightQueue {
    @Shadow
    @Final
    private Long2ObjectLinkedOpenHashMap<ChunkTasks> tasksByChunk;
    @Shadow
    @Final
    private Long2ObjectOpenHashMap<ChunkTasks> inFlightTasks;
    @Shadow
    @Final
    private LongArrayFIFOQueue blockChangeKeys;
    @Shadow
    @Final
    private LongArrayFIFOQueue initialLightKeys;
    @Shadow
    @Final
    private ArrayDeque<ChunkTasks> maintenanceTasks;
    @Shadow
    private int initialLightCount;

    /**
     * @author NovaEngineering
     * @reason Release stale priority entries when the last queued task leaves, as in Pulsar 9203a5e.
     */
    @Overwrite
    private void onTaskRemoved(final ChunkTasks task) {
        if (task.initialLightChunk != null) {
            this.initialLightCount--;
        }
        // All removal paths own the queue monitor here. In-flight tasks live in a separate map.
        if (this.tasksByChunk.isEmpty()) {
            this.blockChangeKeys.clear();
            this.initialLightKeys.clear();
            this.maintenanceTasks.clear();
        }
    }

    /**
     * @author NovaEngineering
     * @reason Signal failed worker tasks through their existing future (Pulsar 40828e6).
     */
    @Overwrite
    void completeTask(final ChunkTasks task) {
        synchronized (this) {
            if (this.inFlightTasks.get(task.chunkCoordinate) == task) {
                this.inFlightTasks.remove(task.chunkCoordinate);
            }
        }
        final Throwable failure = ((PulsarFailureAccess.Task) (Object) task).novaeng$failure();
        if (failure == null) {
            task.onComplete.set(null);
        } else {
            task.onComplete.setException(failure);
        }
    }
}
