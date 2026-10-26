package github.kasuminova.novaeng.common.util;

import com.dhj.actinium.render.terrain.compile.VintageChunkBuildContext;
import net.minecraft.client.renderer.BufferBuilder;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Reads and clears the per-layer vanilla buffers of Actinium's vintage build context.
 *
 * <p>The field holding them is {@code BufferBuilder[] worldRenderers} in older Actinium builds and
 * {@code ChunkBufferBuilder[] worldRenderers} from the change that routed the vanilla dispatcher through the
 * chunk renderer; {@code ChunkBufferBuilder extends BufferBuilder}, so the elements stay usable as
 * {@code BufferBuilder} either way. A {@code @Shadow} declaration is matched by name and descriptor, so one
 * hard-coded array type cannot serve both versions and the mixin fails to apply on the other one. Resolving the
 * field once per class keeps the same build working against either declaration.
 *
 * <p>Every write goes through the array's elements. Replacing the array itself would not work: a
 * {@code BufferBuilder[]} cannot be stored into a field declared as {@code ChunkBufferBuilder[]}.
 */
public final class VintageRendererAccess {

    private static final String RENDERERS_FIELD = "worldRenderers";
    private static final String USED_FIELD = "usedWorldRenderers";

    private static Field renderers;
    private static Field used;

    private VintageRendererAccess() {
    }

    /** The per-layer buffers of the context. */
    public static BufferBuilder[] renderers(final VintageChunkBuildContext context) {
        return read(renderers(), context, RENDERERS_FIELD);
    }

    /** The per-layer flags recording whether a buffer already began drawing. */
    public static boolean[] used(final VintageChunkBuildContext context) {
        return read(used(), context, USED_FIELD);
    }

    /** Returns one layer's buffer to the pool by clearing that slot. */
    public static void clearRenderer(final VintageChunkBuildContext context, final int layer) {
        renderers(context)[layer] = null;
    }

    /** Clears every layer's buffer and used flag, as the mixin does when a build's scratch is released. */
    public static void releaseAll(final VintageChunkBuildContext context) {
        Arrays.fill(renderers(context), null);
        Arrays.fill(used(context), false);
    }

    private static Field renderers() {
        return renderers == null ? renderers = find(RENDERERS_FIELD) : renderers;
    }

    private static Field used() {
        return used == null ? used = find(USED_FIELD) : used;
    }

    @SuppressWarnings("unchecked")
    private static <T> T read(final Field field, final VintageChunkBuildContext context, final String name) {
        try {
            return (T) field.get(context);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to read " + name + " from a vintage build context", e);
        }
    }

    private static Field find(final String name) {
        final Field field;
        try {
            field = VintageChunkBuildContext.class.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Actinium's vintage build context has no " + name
                + "; this NovaEngineering build does not match the installed Actinium version", e);
        }
        field.setAccessible(true);
        return field;
    }
}
