package github.kasuminova.novaeng.client.render;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * Remembers the packed vertex data a CodeChicken Lib model produced, so a renderer that draws the same static model
 * every frame can hand the vertices back instead of running them through the library's per-vertex pipeline again.
 *
 * <p>CodeChicken Lib renders a model by walking its vertices and applying a list of operations to each one, and the
 * draconic renderers hand it nothing but a transformation matrix - so what varies between frames is only the transform,
 * and the vertices themselves are constant. Moving that transform to the GL matrix (see the renderer mixins) leaves the
 * vertex data identical every frame, which is what makes it worth keeping: replaying it costs one
 * {@code addVertexData} per vertex instead of the library's per-vertex transform, UV, normal and lightmap work.</p>
 *
 * <p>The cache is filled by the library itself: the first time a model is drawn the caller lets it render normally and
 * then records what it wrote, so the replayed data is exactly what the library produced. Only the
 * {@code POSITION_TEX} format with a single {@link Matrix4} operation is accepted - the shape whose entire per-frame
 * variation the caller has moved out - and anything else is left to the library.</p>
 */
public final class CclModelGeometryCache {

    private static final Object2ObjectOpenHashMap<Object, int[]> CACHE = new Object2ObjectOpenHashMap<>();
    private static final ThreadLocal<int[]> SCRATCH = new ThreadLocal<>();

    private CclModelGeometryCache() {
    }

    /**
     * Writes the remembered vertices when this model is already known, otherwise leaves the draw to the library.
     *
     * @return true when the geometry was replayed and the caller must not render it again
     */
    public static boolean replay(final Object model, final CCRenderState state, final IVertexOperation[] ops) {
        if (state == null || ops == null || ops.length != 1 || !(ops[0] instanceof Matrix4)) {
            return false;
        }
        final BufferBuilder buffer = state.r;
        if (buffer == null || buffer.getVertexFormat() != DefaultVertexFormats.POSITION_TEX) {
            return false;
        }
        final int[] cached = CACHE.get(model);
        if (cached == null) {
            return false;
        }
        final int intsPerVertex = buffer.getVertexFormat().getIntegerSize();
        int[] scratch = SCRATCH.get();
        if (scratch == null || scratch.length < intsPerVertex) {
            scratch = new int[intsPerVertex];
            SCRATCH.set(scratch);
        }
        for (int i = 0; i < cached.length; i += intsPerVertex) {
            System.arraycopy(cached, i, scratch, 0, intsPerVertex);
            buffer.addVertexData(scratch);
        }
        return true;
    }

    /** Records the vertices the library has just written for this model. */
    public static void remember(final Object model, final CCRenderState state) {
        if (state == null || CACHE.containsKey(model)) {
            return;
        }
        final BufferBuilder buffer = state.r;
        if (buffer == null || buffer.getVertexFormat() != DefaultVertexFormats.POSITION_TEX) {
            return;
        }
        final int vertices = buffer.getVertexCount();
        if (vertices <= 0) {
            return;
        }
        final int[] data = new int[vertices * buffer.getVertexFormat().getIntegerSize()];
        final ByteBuffer byteBuffer = buffer.getByteBuffer().order(ByteOrder.nativeOrder());
        final IntBuffer asInts = byteBuffer.asIntBuffer();
        asInts.get(0, data);
        CACHE.put(model, data);
    }
}
