package github.kasuminova.novaeng.mixin.pulsar;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import github.kasuminova.novaeng.common.performance.PulsarFailureAccess;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

@Mixin(targets = "com.sumirelabs.pulsar.light.ChunkLightCompletion", remap = false)
public abstract class MixinChunkLightCompletion implements PulsarFailureAccess.Completion {
    @Shadow
    @Final
    long generation;
    @Shadow
    @Final
    Chunk chunk;
    @Shadow
    @Final
    SettableFuture<Void> future;
    @Shadow
    volatile boolean published;

    @Shadow
    abstract void finish(boolean published);

    @Override
    public final long novaeng$generation() {
        return this.generation;
    }

    @Override
    public final Chunk novaeng$chunk() {
        return this.chunk;
    }

    @Override
    public final boolean novaeng$forwardRecovery(final PulsarFailureAccess.Completion superseded) {
        try {
            Futures.getDone(this.future);
            return this.published;
        } catch (final ExecutionException failure) {
            superseded.novaeng$fail(failure.getCause());
        } catch (final CancellationException cancelled) {
            // The original listener calls finish(false) for cancelled recovery.
        }
        return false;
    }

    @Override
    public final void novaeng$finish(final boolean published) {
        this.finish(published);
    }

    // Use the same monitor as finish(): the publication bit is written only on that path.
    @Override
    public final synchronized void novaeng$fail(final Throwable failure) {
        this.future.setException(failure);
    }
}
