package github.kasuminova.novaeng.mixin.alfheim;

import dev.redstudio.alfheim.utils.DeduplicatedLongQueue;
import github.kasuminova.novaeng.common.performance.LightingDeduplicationSet;
import github.kasuminova.novaeng.common.performance.RetainedLongFifoQueue;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DeduplicatedLongQueue.class, remap = false)
public abstract class MixinDeduplicatedLongQueue {

    @Shadow
    @Final
    private LongArrayFIFOQueue queue;
    @Shadow
    private LongOpenHashSet set;

    @Redirect(method = "<init>", at = @At(value = "NEW",
        target = "(I)Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;"), require = 1)
    private LongOpenHashSet nova$smallInitialSet(final int capacity) {
        return new LightingDeduplicationSet(Math.min(capacity, 256));
    }

    @Redirect(method = "<init>", at = @At(value = "NEW",
        target = "(I)Lit/unimi/dsi/fastutil/longs/LongArrayFIFOQueue;"), require = 1)
    private LongArrayFIFOQueue nova$retainedQueue(final int capacity) {
        return new RetainedLongFifoQueue(Math.min(capacity, 256));
    }

    /**
     * @author circulation
     * @reason 在原有去重轮次边界复用原始类型表，并释放异常增长后的大数组。
     */
    @Overwrite
    public void newDeduplicationSet() {
        if (this.queue instanceof RetainedLongFifoQueue retained) retained.nextPass();
        this.set = this.set instanceof LightingDeduplicationSet reusable
            ? reusable.nextPass(this.queue.size()) : new LightingDeduplicationSet(this.queue.size());
    }
}
