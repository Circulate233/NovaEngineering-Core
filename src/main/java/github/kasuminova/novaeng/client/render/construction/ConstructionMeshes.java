package github.kasuminova.novaeng.client.render.construction;

import github.kasuminova.novaeng.client.render.construction.ConstructionAnimation.SectionMesh;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GLContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static github.kasuminova.novaeng.client.render.construction.ConstructionAnimation.MIN_OPACITY;
import static github.kasuminova.novaeng.client.render.construction.ConstructionSurface.FLUID;
import static github.kasuminova.novaeng.client.render.construction.ConstructionSurface.HEIGHT;
import static github.kasuminova.novaeng.client.render.construction.ConstructionSurface.SOLID;
import static github.kasuminova.novaeng.client.render.construction.ConstructionSurface.WIDTH;
import static github.kasuminova.novaeng.common.assembly.effect.ConstructionEffectPayload.MAX_LOGICAL_CHUNKS;

final class ConstructionMeshes implements AutoCloseable {
    static final int STRIDE = 3 * Short.BYTES;
    static final Part[] NO_PARTS = new Part[0];
    static final byte WIRE = -1;
    private static final int MAX_UPLOADS = 12;
    private static final int MAX_UPLOAD_BYTES = 1 << 20;
    private static final int STEP_BATCH = 16;
    private static final int MAX_VERTICES = 6 * ConstructionAnimation.SLICE_CELLS * 6;
    private static final int PAGE_VERTICES = (1 << 17) / STRIDE;
    private static final int PAGE_COORD_MASK = (1 << 14) - 1;
    private final ConstructionSurface surface = new ConstructionSurface();
    private final java.lang.foreign.Arena scratch = java.lang.foreign.Arena.ofConfined();
    private final ByteBuffer output = scratch.allocate((long) MAX_VERTICES * STRIDE, Float.BYTES).asByteBuffer().order(ByteOrder.nativeOrder());
    private final Work work = new Work();
    private SectionMesh sampledSection;
    private int sampledVersion;
    private int uploads;
    private int uploadedBytes;

    private static boolean partLive(final SectionMesh section, final Part part) {
        return part.region == WIRE ? !section.task.wireReleased : (section.present & 1L << part.region) != 0;
    }

    void beginFrame() {
        uploads = 0;
        uploadedBytes = 0;
    }

    boolean prefetching() {
        return work.section != null && work.opacity() <= MIN_OPACITY;
    }

    boolean resume(final int frame, final long deadline, final boolean vbo, final boolean allowPrefetch) {
        final var section = work.section;
        if (section == null) return true;
        final var part = work.part;
        final boolean needed = work.opacity() > MIN_OPACITY || allowPrefetch
            && (part.region == WIRE ? section.prepareWire : section.prepareShell && part.solid());
        if (section.seenFrame != frame || section.version != work.version || !needed || !partLive(section, part)) {
            cancel();
            return true;
        }
        return advance(deadline, vbo);
    }

    boolean build(final SectionMesh section, final Part part, final long deadline, final boolean vbo) {
        if (part.version == section.version) return true;
        if (uploads >= MAX_UPLOADS || System.nanoTime() >= deadline) return false;
        work.set(section, part);
        output.clear();
        final boolean reuseSamples = sampledSection == section && sampledVersion == section.version;
        surface.reset(reuseSamples);
        if (!reuseSamples) sampledSection = null;
        return advance(deadline, vbo);
    }

    private boolean advance(final long deadline, final boolean vbo) {
        if (!work.ready) {
            while (!work.ready && System.nanoTime() < deadline) {
                for (int i = 0; i < STEP_BATCH; i++) {
                    if (!surface.step(work, work.material, work)) continue;
                    sampledSection = work.section;
                    sampledVersion = work.version;
                    output.flip();
                    work.ready = true;
                    break;
                }
            }
        }
        if (!work.ready || uploads >= MAX_UPLOADS || System.nanoTime() >= deadline) return false;
        final int bytes = output.remaining();
        if (bytes > MAX_UPLOAD_BYTES - uploadedBytes) return false;
        try {
            final var part = work.part;
            final var mesh = bytes == 0 ? null : work.section.task.arena.store(work.section, output, vbo);
            part.delete();
            part.mesh = mesh;
            part.version = work.version;
            if (bytes != 0) {
                uploads++;
                uploadedBytes += bytes;
            }
        } finally {
            cancel();
        }
        return true;
    }

    void cancel(final ConstructionAnimation task) {
        if (work.section != null && work.section.task == task) cancel();
        if (sampledSection != null && sampledSection.task == task) sampledSection = null;
    }

    void cancel() {
        work.section = null;
        work.part = null;
        Arrays.fill(work.neighborhood, null);
    }

    @Override
    public void close() {
        cancel();
        sampledSection = null;
        scratch.close();
    }

    static final class Part {
        final byte region;
        Mesh mesh;
        int version = -1;
        float opacity;

        Part(final byte region) {
            this.region = region;
        }

        boolean solid() {
            return region >= 0 && region < MAX_LOGICAL_CHUNKS;
        }

        void delete() {
            if (mesh != null) mesh.page.release(mesh.first, mesh.count);
            mesh = null;
            version = -1;
        }
    }

    record Mesh(Page page, int first, int count) {
    }

    static final class Arena implements AutoCloseable {
        final ObjectArrayList<Page> pages = new ObjectArrayList<>();
        final int x, y, z;
        int drawPasses;
        float wireOpacity;

        Arena(final BlockPos origin) {
            x = origin.getX() & -WIDTH;
            y = origin.getY() & -HEIGHT;
            z = origin.getZ() & -WIDTH;
        }

        Mesh store(final SectionMesh section, final ByteBuffer source, final boolean vbo) {
            final int x = this.x + ((section.x * WIDTH - this.x) & ~PAGE_COORD_MASK);
            final int y = this.y + ((section.y * HEIGHT - this.y) & ~PAGE_COORD_MASK);
            final int z = this.z + ((section.z * WIDTH - this.z) & ~PAGE_COORD_MASK);
            final int count = source.remaining() / STRIDE;
            for (int i = 0; i < pages.size(); i++) {
                final var page = pages.get(i);
                if (page.x != x || page.y != y || page.z != z) continue;
                final int offset = page.reserve(count);
                if (offset >= 0) return page.store(offset, count, source);
            }
            final var page = new Page(this, vbo, x, y, z, Math.max(PAGE_VERTICES, count));
            pages.add(page);
            return page.store(page.reserve(count), count, source);
        }

        @Override
        public void close() {
            for (int i = 0; i < pages.size(); i++) pages.get(i).dispose();
            pages.clear();
        }
    }

    static final class Page {
        final ByteBuffer cpu;
        final int id;
        final int x, y, z;
        final ObjectArrayList<Mesh> solid = new ObjectArrayList<>();
        final ObjectArrayList<Part> fading = new ObjectArrayList<>();
        final ObjectArrayList<Mesh> wires = new ObjectArrayList<>();
        private final Arena arena;
        private final boolean coreVbo;
        private final java.lang.foreign.Arena cpuMemory;
        private final IntArrayList free = new IntArrayList();
        private final int capacity;
        private int high;
        private int live;

        Page(final Arena arena, final boolean vbo, final int x, final int y, final int z, final int capacity) {
            this.arena = arena;
            this.x = x;
            this.y = y;
            this.z = z;
            this.capacity = capacity;
            coreVbo = vbo && GLContext.getCapabilities().OpenGL15;
            id = vbo ? OpenGlHelper.glGenBuffers() : 0;
            if (vbo && id == 0) throw new IllegalStateException("Could not allocate a construction vertex buffer");
            cpuMemory = vbo ? null : java.lang.foreign.Arena.ofConfined();
            cpu = cpuMemory == null ? null : cpuMemory.allocate((long) capacity * STRIDE, Float.BYTES).asByteBuffer().order(ByteOrder.nativeOrder());
            if (id == 0) return;
            try {
                OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, id);
                if (coreVbo) {
                    GL15.glBufferData(OpenGlHelper.GL_ARRAY_BUFFER, (long) capacity * STRIDE, OpenGlHelper.GL_STATIC_DRAW);
                } else {
                    ARBVertexBufferObject.glBufferDataARB(OpenGlHelper.GL_ARRAY_BUFFER, (long) capacity * STRIDE, OpenGlHelper.GL_STATIC_DRAW);
                }
            } catch (RuntimeException | Error failure) {
                OpenGlHelper.glDeleteBuffers(id);
                throw failure;
            }
        }

        int reserve(final int count) {
            for (int i = 0; i < free.size(); i += 2) {
                final int offset = free.getInt(i);
                final int available = free.getInt(i + 1);
                if (available < count) continue;
                if (available == count) free.removeElements(i, i + 2);
                else {
                    free.set(i, offset + count);
                    free.set(i + 1, available - count);
                }
                live++;
                return offset;
            }
            final int offset = high;
            if (offset + count > capacity) return -1;
            high += count;
            live++;
            return offset;
        }

        Mesh store(final int offset, final int count, final ByteBuffer source) {
            final int byteOffset = offset * STRIDE;
            try {
                if (id != 0) {
                    OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, id);
                    if (coreVbo) GL15.glBufferSubData(OpenGlHelper.GL_ARRAY_BUFFER, byteOffset, source);
                    else ARBVertexBufferObject.glBufferSubDataARB(OpenGlHelper.GL_ARRAY_BUFFER, byteOffset, source);
                } else {
                    cpu.put(byteOffset, source, 0, source.remaining());
                }
                return new Mesh(this, offset, count);
            } catch (RuntimeException | Error failure) {
                release(offset, count);
                throw failure;
            }
        }

        void release(final int position, final int count) {
            if (--live == 0) {
                dispose();
                arena.pages.remove(this);
                return;
            }
            int start = position;
            int end = position + count;
            int cursor = 0;
            while (cursor < free.size() && free.getInt(cursor) < start) cursor += 2;
            if (cursor > 0 && free.getInt(cursor - 2) + free.getInt(cursor - 1) == start) {
                cursor -= 2;
                start = free.getInt(cursor);
                free.removeElements(cursor, cursor + 2);
            }
            if (cursor < free.size() && free.getInt(cursor) == end) {
                end += free.getInt(cursor + 1);
                free.removeElements(cursor, cursor + 2);
            }
            if (end == high) high = start;
            else {
                free.add(cursor, start);
                free.add(cursor + 1, end - start);
            }
        }

        private void dispose() {
            if (id != 0) OpenGlHelper.glDeleteBuffers(id);
            if (cpuMemory != null) cpuMemory.close();
        }
    }

    private final class Work implements ConstructionSurface.Cells, ConstructionSurface.Vertex {
        private final byte[][] neighborhood = new byte[27][];
        private SectionMesh section;
        private Part part;
        private int version;
        private byte material;
        private int x, y, z;
        private boolean ready;

        void set(final SectionMesh section, final Part part) {
            this.section = section;
            this.part = part;
            version = section.version;
            material = part.region == WIRE ? 0 : ConstructionSurface.groupMaterial(part.region % MAX_LOGICAL_CHUNKS, part.solid() ? SOLID : FLUID);
            final var arena = section.task.arena;
            x = (section.x * WIDTH - arena.x) & PAGE_COORD_MASK;
            y = (section.y * HEIGHT - arena.y) & PAGE_COORD_MASK;
            z = (section.z * WIDTH - arena.z) & PAGE_COORD_MASK;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        neighborhood[(dy + 1) * 9 + (dz + 1) * 3 + dx + 1] = section.task.cells(section.x + dx, section.y + dy, section.z + dz);
                    }
                }
            }
            ready = false;
        }

        float opacity() {
            return part.opacity;
        }

        @Override
        public byte at(final int x, final int y, final int z) {
            final var cells = neighborhood[((y >> 3) + 1) * 9 + ((z >> 4) + 1) * 3 + (x >> 4) + 1];
            return cells == null ? 0 : cells[(x & 15) | ((z & 15) << 4) | ((y & 7) << 8)];
        }

        @Override
        public void add(final int x, final int y, final int z) {
            output.putShort((short) (this.x + x)).putShort((short) (this.y + y)).putShort((short) (this.z + z));
        }
    }
}
