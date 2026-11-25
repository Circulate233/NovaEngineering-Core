package github.kasuminova.novaeng.client.geometry.block;

import net.minecraft.client.renderer.block.model.IBakedModel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** A CTM wrapper visits its parent for texture callbacks and discards the returned geometry. */
public final class CtmMaterialProbe {
    private static final ScopedValue<CtmMaterialProbe> ACTIVE = ScopedValue.newInstance();
    private static final ScopedValue<Object> UNPUBLISHED_MODEL = ScopedValue.newInstance();

    private final Object ctm;
    private final Map<Object, IBakedModel> baked = new HashMap<>();
    private Object textureGetter;

    private CtmMaterialProbe(final Object ctm) {
        this.ctm = ctm;
    }

    public static IBakedModel run(final Object ctm, final Supplier<IBakedModel> operation) {
        return ScopedValue.where(ACTIVE, new CtmMaterialProbe(ctm)).call(operation::get);
    }

    public static void identifyGetter(final Object ctm, final Object textureGetter) {
        if (!ACTIVE.isBound()) return;
        final CtmMaterialProbe probe = ACTIVE.get();
        if (probe.ctm == ctm) probe.textureGetter = textureGetter;
    }

    public static CtmMaterialProbe forGetter(final Object textureGetter) {
        if (textureGetter == null || !ACTIVE.isBound()) return null;
        final CtmMaterialProbe probe = ACTIVE.get();
        return probe.textureGetter == textureGetter ? probe : null;
    }

    public static boolean isUnpublished(final Object model, final Object getter) {
        return UNPUBLISHED_MODEL.isBound() && UNPUBLISHED_MODEL.get() == model && forGetter(getter) != null;
    }

    public IBakedModel bake(final Object model, final Object forgeCacheKey, final Supplier<IBakedModel> operation) {
        final IBakedModel existing = baked.get(forgeCacheKey);
        if (existing != null) return existing;
        // A nested bake can insert other keys before this one is complete.
        final IBakedModel result = ScopedValue.where(UNPUBLISHED_MODEL, model).call(operation::get);
        baked.put(forgeCacheKey, result);
        return result;
    }
}
