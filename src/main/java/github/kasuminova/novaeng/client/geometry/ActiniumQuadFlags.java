package github.kasuminova.novaeng.client.geometry;

import dhj.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import net.minecraft.client.renderer.block.model.BakedQuad;

/** Keeps Actinium's types out of the vanilla quad path. */
public final class ActiniumQuadFlags {
    private static final boolean AVAILABLE = ActiniumQuadFlags.class.getClassLoader()
        .getResource("dhj/embeddedt/embeddium/impl/model/quad/BakedQuadView.class") != null;

    private ActiniumQuadFlags() {
    }

    public static int get(final BakedQuad quad) {
        return AVAILABLE ? QuadView.get(quad) : 0;
    }

    public static void add(final BakedQuad quad, final int flags) {
        if (AVAILABLE && flags != 0) QuadView.add(quad, flags);
    }

    public static void copyMasked(final BakedQuad source, final BakedQuad target, final int mask) {
        if (AVAILABLE) QuadView.copyMasked(source, target, mask);
    }

    private static final class QuadView {
        private static int get(final BakedQuad quad) {
            return quad instanceof BakedQuadView view ? view.getFlags() : 0;
        }

        private static void add(final BakedQuad quad, final int flags) {
            if (quad instanceof BakedQuadView view) view.addFlags(flags);
        }

        private static void copyMasked(final BakedQuad source, final BakedQuad target, final int mask) {
            if (source instanceof BakedQuadView from && target instanceof BakedQuadView to) {
                to.addFlags(from.getFlags() & mask);
            }
        }
    }
}
