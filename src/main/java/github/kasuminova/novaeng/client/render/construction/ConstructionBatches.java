package github.kasuminova.novaeng.client.render.construction;

import github.kasuminova.novaeng.client.render.construction.ConstructionAnimation.SectionMesh;
import github.kasuminova.novaeng.client.render.construction.ConstructionMeshes.Arena;
import github.kasuminova.novaeng.client.render.construction.ConstructionMeshes.Mesh;
import github.kasuminova.novaeng.client.render.construction.ConstructionMeshes.Page;
import github.kasuminova.novaeng.client.render.construction.ConstructionMeshes.Part;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

import static github.kasuminova.novaeng.client.render.construction.ConstructionAnimation.MIN_OPACITY;
import static github.kasuminova.novaeng.client.render.construction.ConstructionMeshes.STRIDE;

final class ConstructionBatches implements AutoCloseable {
    private static final int OPAQUE = 1;
    private static final int FADING = 2;
    private static final int WIRE = 4;
    private static final int MAX_RANGES = 512;
    private static final float RED = 244 / 255F;
    private static final float GREEN = 248 / 255F;

    private final ObjectArrayList<Arena> arenas = new ObjectArrayList<>();
    private final java.lang.foreign.Arena memory = java.lang.foreign.Arena.ofConfined();
    private final IntBuffer starts = memory.allocate(ValueLayout.JAVA_INT, MAX_RANGES).asByteBuffer().order(ByteOrder.nativeOrder()).asIntBuffer();
    private final IntBuffer counts = memory.allocate(ValueLayout.JAVA_INT, MAX_RANGES).asByteBuffer().order(ByteOrder.nativeOrder()).asIntBuffer();
    private final boolean multiDraw = GLContext.getCapabilities().OpenGL14;
    private final int[] activeAlpha = new int[256];
    private final int[] firstAlpha = new int[256];
    private Part[] fadingParts = new Part[64];
    private int[] nextPart = new int[fadingParts.length];
    private int passes;

    ConstructionBatches() {
        Arrays.fill(firstAlpha, -1);
    }

    private static void addShell(final Arena arena, final Part part, final int version) {
        final var mesh = part.mesh;
        if (mesh == null || part.version != version || part.opacity <= MIN_OPACITY) return;
        if (part.opacity == 1) {
            arena.drawPasses |= OPAQUE;
            mesh.page().solid.add(mesh);
        } else {
            arena.drawPasses |= FADING;
            mesh.page().fading.add(part);
        }
    }

    private static void bind(final Page page) {
        if (page.id != 0) {
            OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, page.id);
            GlStateManager.glVertexPointer(3, GL11.GL_SHORT, STRIDE, 0);
        } else {
            if (OpenGlHelper.vboSupported) OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
            GlStateManager.glVertexPointer(3, GL11.GL_SHORT, STRIDE, page.cpu);
        }
    }

    private static void color(final float opacity) {
        GlStateManager.color(RED, GREEN, 1, opacity);
    }

    void collect(final ObjectArrayList<SectionMesh> visible) {
        for (int i = 0; i < visible.size(); i++) {
            final var section = visible.get(i);
            final var arena = section.task.arena;
            final int previous = arena.drawPasses;
            for (final var part : section.parts) addShell(arena, part, section.version);
            final var wire = section.wire;
            if (wire.opacity > MIN_OPACITY && wire.mesh != null && wire.version == section.version) {
                arena.drawPasses |= WIRE;
                arena.wireOpacity = wire.opacity;
                wire.mesh.page().wires.add(wire.mesh);
            }
            if (previous == 0 && arena.drawPasses != 0) arenas.add(arena);
            passes |= arena.drawPasses;
        }
    }

    boolean empty() {
        return passes == 0;
    }

    void draw(final double x, final double y, final double z, final ConstructionGlState state) {
        if ((passes & (OPAQUE | FADING)) != 0) {
            GlStateManager.enablePolygonOffset();
            GlStateManager.doPolygonOffset(-1.0F, -10.0F);
            if ((passes & OPAQUE) != 0) {
                GlStateManager.disableBlend();
                GlStateManager.depthMask(true);
                color(1);
                drawPass(OPAQUE, x, y, z);
            }
            if ((passes & FADING) != 0) {
                GlStateManager.enableBlend();
                GlStateManager.depthMask(false);
                drawPass(FADING, x, y, z);
            }
            GlStateManager.disablePolygonOffset();
        }
        if ((passes & WIRE) != 0) {
            state.prepareWireDepth();
            GlStateManager.enableBlend();
            GlStateManager.depthMask(false);
            drawPass(WIRE, x, y, z);
        }
    }

    private void drawPass(final int pass, final double x, final double y, final double z) {
        for (int i = 0; i < arenas.size(); i++) {
            final var arena = arenas.get(i);
            if ((arena.drawPasses & pass) == 0) continue;
            if (pass == WIRE) color(arena.wireOpacity);
            for (int pageIndex = 0; pageIndex < arena.pages.size(); pageIndex++) {
                final var page = arena.pages.get(pageIndex);
                final var meshes = pass == WIRE ? page.wires : page.solid;
                if (pass == FADING ? page.fading.isEmpty() : meshes.isEmpty()) continue;
                bind(page);
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.translate(page.x - x, page.y - y, page.z - z);
                    if (pass == FADING) {
                        drawFading(page.fading);
                    } else {
                        final int mode = pass == WIRE ? GL11.GL_LINES : GL11.GL_TRIANGLES;
                        for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++)
                            add(meshes.get(meshIndex), mode);
                        submit(mode);
                    }
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    private void drawFading(final ObjectArrayList<Part> fading) {
        final int size = fading.size();
        if (size > fadingParts.length) {
            fadingParts = new Part[Integer.highestOneBit(size - 1) << 1];
            nextPart = new int[fadingParts.length];
        }
        int alphas = 0;
        // Equal alpha shares one batch directly from cached mesh ranges.
        for (int i = size - 1; i >= 0; i--) {
            final var part = fading.get(i);
            final int alpha = Math.round(part.opacity * 255F);
            final int previous = firstAlpha[alpha];
            if (previous < 0) activeAlpha[alphas++] = alpha;
            firstAlpha[alpha] = i;
            nextPart[i] = previous;
            fadingParts[i] = part;
        }
        try {
            for (int i = 0; i < alphas; i++) {
                final int alpha = activeAlpha[i];
                color(alpha / 255F);
                for (int p = firstAlpha[alpha]; p >= 0; p = nextPart[p]) add(fadingParts[p].mesh, GL11.GL_TRIANGLES);
                submit(GL11.GL_TRIANGLES);
            }
        } finally {
            for (int i = 0; i < alphas; i++) firstAlpha[activeAlpha[i]] = -1;
            Arrays.fill(fadingParts, 0, size, null);
        }
    }

    private void add(final Mesh mesh, final int mode) {
        final int size = starts.position();
        if (size != 0 && starts.get(size - 1) + counts.get(size - 1) == mesh.first()) {
            counts.put(size - 1, counts.get(size - 1) + mesh.count());
        } else {
            if (size == starts.capacity()) submit(mode);
            starts.put(mesh.first());
            counts.put(mesh.count());
        }
    }

    private void submit(final int mode) {
        final int size = starts.position();
        if (size == 0) return;
        if (size > 1 && multiDraw) GL14.glMultiDrawArrays(mode, starts.flip(), counts.flip());
        else for (int i = 0; i < size; i++) GlStateManager.glDrawArrays(mode, starts.get(i), counts.get(i));
        starts.clear();
        counts.clear();
    }

    void clear() {
        for (int i = 0; i < arenas.size(); i++) {
            final var arena = arenas.get(i);
            for (int p = 0; p < arena.pages.size(); p++) {
                final var page = arena.pages.get(p);
                page.solid.clear();
                page.fading.clear();
                page.wires.clear();
            }
            arena.drawPasses = 0;
        }
        arenas.clear();
        starts.clear();
        counts.clear();
        passes = 0;
    }

    @Override
    public void close() {
        clear();
        memory.close();
    }
}
