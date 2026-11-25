package github.kasuminova.novaeng.common.performance;

import com.sumirelabs.pulsar.light.ChunkTasks;
import net.minecraft.world.chunk.Chunk;

/**
 * Bridges the package-private Pulsar 0.6.2 failure state between its owning classes.
 */
public interface PulsarFailureAccess {
    interface ChunkMap {
        void novaeng$bindLighting(Coordinator coordinator);

        Coordinator novaeng$lighting();
    }

    interface Coordinator {
        void novaeng$failTask(ChunkTasks task, Throwable failure);

        boolean novaeng$hasFailedNear(int chunkX, int chunkZ);

        boolean novaeng$retryEdges(ChunkTasks task, int chunkX, int chunkZ, String engineName);
    }

    interface Task {
        Throwable novaeng$failure();

        void novaeng$setFailure(Throwable failure);
    }

    interface Completion {
        long novaeng$generation();

        Chunk novaeng$chunk();

        boolean novaeng$forwardRecovery(Completion superseded);

        void novaeng$finish(boolean published);

        void novaeng$fail(Throwable failure);
    }
}
