package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.mmce.client.util.BufferBuilderPool;
import github.kasuminova.novaeng.client.memory.MmceBufferPoolBudget;
import net.minecraft.client.renderer.BufferBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;

@Mixin(value = BufferBuilderPool.class, remap = false)
public abstract class MixinBufferBuilderPoolMemory {
    @Shadow
    @Final
    private static NavigableMap<Integer, Queue<BufferBuilder>> POOL;
    @Shadow
    @Final
    private static List<WeakReference<BufferBuilder>> BUFFERS;
    @Unique
    private static int nova$discarded;

    @Redirect(method = "borrowBuffer", at = @At(value = "INVOKE",
        target = "Ljava/util/NavigableMap;ceilingEntry(Ljava/lang/Object;)Ljava/util/Map$Entry;"), require = 1)
    private static Map.Entry<Integer, Queue<BufferBuilder>> nova$avoidOversizedBuffer(
        final NavigableMap<Integer, Queue<BufferBuilder>> pool, final Object requested) {
        final int size = (Integer) requested;
        final var entry = pool.ceilingEntry(size);
        return entry != null && entry.getKey() <= Math.max(65536L, (long) size * 2) ? entry : null;
    }

    @Redirect(method = "borrowBuffer", at = @At(value = "INVOKE",
        target = "Ljava/util/Queue;poll()Ljava/lang/Object;"), require = 1)
    private static Object nova$accountBorrow(final Queue<BufferBuilder> queue) {
        final BufferBuilder buffer = queue.poll();
        if (buffer != null) MmceBufferPoolBudget.borrowed(buffer.getByteBuffer().capacity());
        return buffer;
    }

    @Redirect(method = "returnBuffer", at = @At(value = "INVOKE",
        target = "Ljava/util/Queue;offer(Ljava/lang/Object;)Z"), require = 2)
    private static boolean nova$boundIdleBuffers(final Queue<BufferBuilder> queue, final Object value) {
        final BufferBuilder buffer = (BufferBuilder) value;
        final int capacity = buffer.getByteBuffer().capacity();
        if (queue.size() < 8 && MmceBufferPoolBudget.retain(capacity)) return queue.offer(buffer);
        if (queue.isEmpty()) POOL.remove(capacity, queue);
        if (++nova$discarded >= 64) {
            nova$discarded = 0;
            nova$removeCollectedBuffers();
        }
        return false;
    }

    @Unique
    private static synchronized void nova$removeCollectedBuffers() {
        BUFFERS.removeIf(bufferBuilderWeakReference -> bufferBuilderWeakReference.get() == null);
    }

    /**
     * @author NovaEngineering
     * @reason Coordinate weak-reference pruning with the pool's existing borrow/return monitor.
     */
    @Overwrite
    public static long getBufferMemUsage() {
        long bytes = 0;
        final var iterator = BUFFERS.iterator();
        while (iterator.hasNext()) {
            final BufferBuilder buffer = iterator.next().get();
            if (buffer == null) iterator.remove();
            else bytes += buffer.getByteBuffer().capacity();
        }
        return bytes;
    }

    /**
     * @author NovaEngineering
     * @reason Exclude collected builders without racing weak-reference iteration.
     */
    @Overwrite
    public static int getCreatedBuffers() {
        nova$removeCollectedBuffers();
        return BUFFERS.size();
    }
}
