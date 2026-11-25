package github.kasuminova.novaeng.mixin.pulsar;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.IdentityHashMap;

@Mixin(targets = "com.sumirelabs.pulsar.light.ChunkTaskReservations", remap = false)
public abstract class MixinChunkTaskReservations {
    @Shadow
    @Final
    private IdentityHashMap<Object, LongArrayList> occupied;
    // All callers hold the dispatcher monitor. An empty primitive list holds no world reference.
    @Unique
    private LongArrayList novaeng$spareCenters;

    @Shadow
    abstract boolean available(Object world, long center);

    /**
     * @author NovaEngineering
     * @reason Reuse one empty primitive list across reservations.
     */
    @Overwrite
    void reserve(final Object world, final long center) {
        if (!this.available(world, center)) throw new IllegalStateException("Overlapping lighting jobs");
        LongArrayList centers = this.occupied.get(world);
        if (centers == null) {
            centers = this.novaeng$spareCenters;
            if (centers == null) {
                centers = new LongArrayList(1);
            } else {
                this.novaeng$spareCenters = null;
            }
            this.occupied.put(world, centers);
        }
        centers.add(center);
    }

    /**
     * @author NovaEngineering
     * @reason Remove the world immediately but keep its empty storage available for reuse.
     */
    @Overwrite
    void release(final Object world, final long center) {
        final LongArrayList centers = this.occupied.get(world);
        if (centers == null || !centers.rem(center)) throw new IllegalStateException("Lighting reservation missing");
        if (centers.isEmpty()) {
            this.occupied.remove(world);
            if (this.novaeng$spareCenters == null) this.novaeng$spareCenters = centers;
        }
    }
}
