package github.kasuminova.novaeng.mixin.loliasm;

import github.kasuminova.novaeng.common.performance.StripedIntArrayPool;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zone.rong.loliasm.LoliLogger;
import zone.rong.loliasm.bakedquad.LoliVertexDataPool;

@Mixin(value = LoliVertexDataPool.class, remap = false)
public abstract class MixinLoliVertexDataPool {
    @Shadow
    private static ObjectOpenCustomHashSet<int[]> POOL;
    @Shadow
    private static int deduplicatedCount;
    @Shadow
    private static int uniqueCount;

    @Unique
    private static volatile StripedIntArrayPool nova$pool = new StripedIntArrayPool(64, 1 << 19);

    /**
     * @author circulation
     * @reason Intern parallel baking results with one content hash and independent shard locks.
     */
    @Overwrite
    public static int[] canonicalize(final int[] value) {
        final StripedIntArrayPool pool = nova$pool;
        return pool == null ? value : pool.canonicalize(value);
    }

    /**
     * @author circulation
     * @reason Preserve unpacked-quad bypass while using the concurrent pool.
     */
    @Overwrite
    public static int[] canonicalize(final int[] value, final BakedQuad quad) {
        return quad instanceof UnpackedBakedQuad ? value : canonicalize(value);
    }

    /**
     * @author circulation
     * @reason Complete accepted insertions before publishing final counts and releasing the pool.
     */
    @Overwrite
    public static synchronized void invalidate() {
        final StripedIntArrayPool pool = nova$pool;
        if (pool != null) {
            final StripedIntArrayPool.Statistics statistics = pool.close();
            deduplicatedCount = (int) statistics.processed();
            uniqueCount = statistics.unique();
            POOL = null;
            nova$pool = null;
            LoliLogger.instance.warn("Clearing LoliVertexDataPool");
        }
    }

    /**
     * @author circulation
     * @reason Aggregate per-shard counters when queried instead of contending on one counter for every quad.
     */
    @Overwrite
    public static int getDeduplicatedCount() {
        final StripedIntArrayPool pool = nova$pool;
        return pool == null ? deduplicatedCount : (int) pool.processedCount();
    }

    @Redirect(method = "onDebugList", at = @At(value = "FIELD",
        target = "Lzone/rong/loliasm/bakedquad/LoliVertexDataPool;deduplicatedCount:I", opcode = Opcodes.GETSTATIC), require = 2)
    private static int nova$debugProcessedCount() {
        return getDeduplicatedCount();
    }
}
