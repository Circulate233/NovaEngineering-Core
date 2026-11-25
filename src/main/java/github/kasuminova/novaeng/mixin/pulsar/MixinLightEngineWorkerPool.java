package github.kasuminova.novaeng.mixin.pulsar;

import com.sumirelabs.pulsar.light.engine.PulsarEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ConcurrentLinkedDeque;

@Mixin(targets = "com.sumirelabs.pulsar.light.LightEngineWorker", remap = false)
public abstract class MixinLightEngineWorkerPool {
    @Shadow
    @Final
    private int maxCachedEngines;
    @Shadow
    @Final
    private ConcurrentLinkedDeque<PulsarEngine> enginePool;
    @Shadow
    private PulsarEngine singleEngine;
    @Shadow
    private boolean singleEngineInUse;

    // Reuse the worker's existing pool as the private monitor; engines run outside it.
    @Unique
    private PulsarEngine[] novaeng$idleEngines;
    @Unique
    private int novaeng$idleCount;

    @Redirect(method = "acquireEngine", at = @At(value = "INVOKE",
        target = "Ljava/util/concurrent/ConcurrentLinkedDeque;pollFirst()Ljava/lang/Object;"), require = 1)
    private Object novaeng$takeIdleEngine(final ConcurrentLinkedDeque<PulsarEngine> pool) {
        if (this.maxCachedEngines == 0) return null;
        synchronized (pool) {
            if (this.novaeng$idleCount == 0) return null;
            final int slot = --this.novaeng$idleCount;
            final PulsarEngine engine = this.novaeng$idleEngines[slot];
            this.novaeng$idleEngines[slot] = null;
            return engine;
        }
    }

    /**
     * @author NovaEngineering
     * @reason Recycle engines without a linked node allocation or a traversal to count the idle slots.
     */
    @Overwrite
    private void releaseEngine(final PulsarEngine engine) {
        if (engine == this.singleEngine) {
            this.singleEngineInUse = false;
            return;
        }
        final int capacity = this.maxCachedEngines;
        if (capacity == 0) return;
        synchronized (this.enginePool) {
            if (this.novaeng$idleCount == capacity) return;
            if (this.novaeng$idleEngines == null) this.novaeng$idleEngines = new PulsarEngine[capacity];
            this.novaeng$idleEngines[this.novaeng$idleCount++] = engine;
        }
    }
}
