package github.kasuminova.novaeng.mixin.pulsar;

import com.sumirelabs.pulsar.util.SnapshotChunkMap;
import github.kasuminova.novaeng.common.performance.PulsarFailureAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = SnapshotChunkMap.class, remap = false)
public abstract class MixinSnapshotChunkMap implements PulsarFailureAccess.ChunkMap {
    @Unique
    private PulsarFailureAccess.Coordinator novaeng$lighting;

    @Override
    public final void novaeng$bindLighting(final PulsarFailureAccess.Coordinator coordinator) {
        this.novaeng$lighting = coordinator;
    }

    @Override
    public final PulsarFailureAccess.Coordinator novaeng$lighting() {
        return this.novaeng$lighting;
    }
}
