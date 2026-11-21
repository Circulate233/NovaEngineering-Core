package github.kasuminova.novaeng.common.performance;

import net.minecraft.client.renderer.block.model.BakedQuad;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/**
 * Reentrant, thread-owned scratch; no quad or texture is retained after addSprites returns.
 */
public final class JourneyMapQuadScratch {
    private static final ThreadLocal<JourneyMapQuadScratch> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<JourneyMapQuadScratch> IDLE = new ThreadLocal<>();
    public final ReferenceOpenHashSet<BakedQuad> set = new ReferenceOpenHashSet<>();
    private JourneyMapQuadScratch previous;

    private JourneyMapQuadScratch() {
    }

    public static JourneyMapQuadScratch begin() {
        JourneyMapQuadScratch scratch = IDLE.get();
        if (scratch == null) scratch = new JourneyMapQuadScratch();
        else IDLE.set(null);
        scratch.previous = ACTIVE.get();
        ACTIVE.set(scratch);
        return scratch;
    }

    public static JourneyMapQuadScratch current() {
        return ACTIVE.get();
    }

    public void close() {
        set.clear();
        // Reuse the ThreadLocal entry too; only the value owns model references.
        ACTIVE.set(previous);
        previous = null;
        if (IDLE.get() == null) IDLE.set(this);
    }
}
