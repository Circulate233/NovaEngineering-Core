package github.kasuminova.novaeng.mixin.journeymap;

import journeymap.common.chunk.JMChunkCache;
import journeymap.common.chunk.JMChunkSnapshot;
import journeymap.common.chunk.JMChunkStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drains JourneyMap's pending chunk writes in one pass.
 *
 * <p>The original walks the pending map by rebuilding its iterator on every loop iteration: it takes one entry,
 * removes it, writes it, then goes back to the top and asks the map for a fresh iterator. That is one
 * {@code ConcurrentHashMap} entry iterator per chunk written, and the map is only ever asked for one at a time -
 * the write itself is what the loop is for, not the iteration.</p>
 *
 * <p>A single iterator walks the same entries. Removal still goes through the map (the conditional
 * remove-or-ignore the original used) rather than {@code Iterator.remove}, so an entry another thread replaces
 * between the visit and the removal is still kept, exactly as before. What changes is the order the queue is
 * drained in: the original restarts from the beginning after every write, so a chunk added while a write is in
 * flight could be picked up before the ones already waiting; a single iterator stays in insertion order for as
 * long as it is draining. Both end with the map empty, so the choice only shows up under continuous load.</p>
 */
@Mixin(value = JMChunkCache.class, remap = false)
public abstract class MixinJMChunkCache {

    @Shadow
    @Final
    private ConcurrentHashMap<Long, JMChunkSnapshot> pendingWrites;

    @Shadow
    private JMChunkStorage storage;

    @Overwrite(remap = false)
    private void drainPendingWrites() {
        final Iterator<Entry<Long, JMChunkSnapshot>> iterator = this.pendingWrites.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<Long, JMChunkSnapshot> next = iterator.next();
            if (this.pendingWrites.remove(next.getKey(), next.getValue())) {
                final JMChunkStorage stor = this.storage;
                if (stor != null) {
                    stor.write(next.getValue());
                }
            }
        }
    }
}
