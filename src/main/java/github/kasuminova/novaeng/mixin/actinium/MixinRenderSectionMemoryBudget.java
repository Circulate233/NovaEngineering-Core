package github.kasuminova.novaeng.mixin.actinium;

import dhj.embeddedt.embeddium.impl.render.chunk.ChunkUpdateType;
import dhj.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import dhj.embeddedt.embeddium.impl.render.chunk.compile.executor.ChunkJobCollector;
import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.memory.BuildMemory;
import github.kasuminova.novaeng.common.performance.PerformanceMetrics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class MixinRenderSectionMemoryBudget {
    @Redirect(method = "submitRebuildTasks", at = @At(value = "INVOKE",
        target = "Ldhj/embeddedt/embeddium/impl/render/chunk/compile/executor/ChunkJobCollector;canOffer()Z"), require = 1)
    private boolean nova$admitBeforeSnapshot(final ChunkJobCollector called,
                                            final ChunkJobCollector collector, final ChunkUpdateType type) {
        if (!called.canOffer()) { return false; }
        if (type.isImportant() || BuildMemory.BUDGET.canSchedule(
            (long) NovaEngCoreConfig.PERFORMANCE.chunkBuildMemoryBudgetMiB << 20)) {
            return true;
        }
        // Before queue.remove/createRebuildTask: pendingUpdate and the queue stay intact.
        PerformanceMetrics.add(PerformanceMetrics.Counter.BUILD_DEFERRED, 1);
        return false;
    }
}
