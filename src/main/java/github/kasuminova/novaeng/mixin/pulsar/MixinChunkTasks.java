package github.kasuminova.novaeng.mixin.pulsar;

import com.sumirelabs.pulsar.light.ChunkTasks;
import github.kasuminova.novaeng.common.performance.PulsarFailureAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ChunkTasks.class, remap = false)
public abstract class MixinChunkTasks implements PulsarFailureAccess.Task {
    @Unique
    private Throwable novaeng$failure;

    @Override
    public final Throwable novaeng$failure() {
        return this.novaeng$failure;
    }

    @Override
    public final void novaeng$setFailure(final Throwable failure) {
        this.novaeng$failure = failure;
    }
}
