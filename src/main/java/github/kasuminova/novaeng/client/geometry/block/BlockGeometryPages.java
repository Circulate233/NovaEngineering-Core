package github.kasuminova.novaeng.client.geometry.block;

import github.kasuminova.novaeng.client.geometry.ActiniumQuadFlags;
import github.kasuminova.novaeng.client.geometry.GeometryGeneration;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * One bake owns the indices; models retain only sealed pages and unsigned page-local bindings.
 */
final class BlockGeometryPages {
    static final int PAGE_SIZE = 1 << 12;
    static final Sequence EMPTY = new PageSequence(null, new short[0]);
    // Indices are bounded by the current page, never by an entire resource reload.
    private static final int WRITERS = Integer.highestOneBit(Math.min(8, Runtime.getRuntime().availableProcessors()));
    private final Writer[] writers;

    BlockGeometryPages() {
        this(WRITERS);
    }

    BlockGeometryPages(final int writerCount) {
        writers = new Writer[writerCount];
        for (int i = 0; i < writers.length; i++) writers[i] = new Writer();
    }

    static BlockGeometryPages active() {
        return GeometryGeneration.attachment(BlockGeometryPages.class, BlockGeometryPages::new, BlockGeometryPages::seal);
    }

    Sequence bind(final Shape[] shapes, final Material[] materials, final int offset, final int count) {
        if (count == 0) return EMPTY;
        if (count <= 2) return bind(shapes[offset], materials[offset], count == 1 ? null : shapes[offset + 1],
            count == 1 ? null : materials[offset + 1]);
        final int hash = shapes[offset].hashCode();
        return writers[(hash ^ (hash >>> 16)) & (writers.length - 1)].bind(shapes, materials, offset, count);
    }

    Sequence bind(final Shape firstShape, final Material firstMaterial, final Shape secondShape, final Material secondMaterial) {
        final int hash = firstShape.hashCode();
        return writers[(hash ^ (hash >>> 16)) & (writers.length - 1)].bind(firstShape, firstMaterial, secondShape, secondMaterial);
    }

    BlockGeometryRecord capture(final int[] words, final int stride, final int position) {
        final int hash = (Arrays.hashCode(words) * 31 + stride) * 31 + position;
        return writers[(hash ^ (hash >>> 16)) & (writers.length - 1)].capture(words, stride, position, hash);
    }

    Material material(final BlockGeometryRecord.Columns columns, final BakedQuad quad) {
        final int flags = ActiniumQuadFlags.get(quad);
        final int hash = Material.hash(columns, quad.getSprite(), quad.getTintIndex(), quad.getFace(), quad.shouldApplyDiffuseLighting(),
            quad.getFormat(), flags);
        return writers[(hash ^ (hash >>> 16)) & (writers.length - 1)].material(columns, quad, flags, hash);
    }

    void seal() {
        // Keep the index local to the reload barrier: models retain pages, never this pool.
        // A late producer holding this attachment uses the closed writer path below.
        final CanonicalRecords canonical = new CanonicalRecords();
        for (final Writer writer : writers) writer.seal(canonical);
    }

    interface Sequence {
        int size();

        BakedQuad quad(int index);
    }

    private static final class Writer {
        // These bounded indices do not touch page state or each other. Keep their
        // encoding/equality work from holding up an unrelated page bind.
        private final Object captureLock = new Object();
        private final Object materialLock = new Object();
        private Page current;
        private List<WeakReference<Page>> completed;
        private Int2ObjectOpenHashMap<BlockGeometryRecord> captures;
        private Int2ObjectOpenHashMap<Material> capturedMaterials;
        private volatile boolean closed;

        BlockGeometryRecord capture(final int[] words, final int stride, final int position, final int hash) {
            synchronized (captureLock) {
                if (closed) return BlockGeometryRecord.capture(words, stride, position);
                if (captures == null) captures = new Int2ObjectOpenHashMap<>();
                final BlockGeometryRecord existing = captures.get(hash);
                // Hashes only select a candidate; equality checks every source word before reuse.
                if (existing != null && existing.matches(words, stride, position)) return existing;
                final BlockGeometryRecord record = BlockGeometryRecord.capture(words, stride, position);
                if (captures.size() == PAGE_SIZE) captures.clear();
                captures.put(hash, record);
                return record;
            }
        }

        Material material(final BlockGeometryRecord.Columns columns, final BakedQuad quad, final int flags, final int hash) {
            synchronized (materialLock) {
                if (closed) return new Material(columns, quad, flags);
                if (capturedMaterials == null) capturedMaterials = new Int2ObjectOpenHashMap<>();
                final Material existing = capturedMaterials.get(hash);
                if (existing != null && existing.matches(columns, quad, flags)) return existing;
                final Material material = new Material(columns, quad, flags);
                if (capturedMaterials.size() == PAGE_SIZE) capturedMaterials.clear();
                capturedMaterials.put(hash, material);
                return material;
            }
        }

        private Page page(final Shape shape, final Material material) {
            if (current == null || !current.fits(shape, material)) {
                if (current != null) {
                    current.seal();
                    if (!closed) {
                        if (completed == null) completed = new ObjectArrayList<>();
                        completed.add(new WeakReference<>(current));
                    }
                }
                current = new Page();
            }
            return current;
        }

        synchronized Sequence bind(final Shape firstShape, final Material firstMaterial, final Shape secondShape, final Material secondMaterial) {
            final Page first = page(firstShape, firstMaterial);
            final long firstIds = first.shapeId(firstShape) | (long) first.materialId(firstMaterial) << 12;
            final Sequence result;
            if (secondShape == null) result = first.sequence(firstIds, 1);
            else {
                final Page second = page(secondShape, secondMaterial);
                final long secondIds = second.shapeId(secondShape) | (long) second.materialId(secondMaterial) << 12;
                result = first == second ? first.sequence(firstIds | secondIds << 24, 2)
                    : new JoinedSequence(new Sequence[]{first.sequence(firstIds, 1), second.sequence(secondIds, 1)});
            }
            if (closed) {
                current.seal();
                current = null;
            }
            return result;
        }

        synchronized Sequence bind(final Shape[] shapes, final Material[] materials, final int offset, final int count) {
            final short[] ids = new short[count * 2];
            Page run = null;
            int start = 0;
            List<Sequence> parts = null;
            for (int index = 0; index < count; index++) {
                final Shape shape = shapes[offset + index];
                final Material material = materials[offset + index];
                page(shape, material);
                if (run != current) {
                    if (run != null) {
                        if (parts == null) parts = new ObjectArrayList<>();
                        parts.add(run.sequence(Arrays.copyOfRange(ids, start * 2, index * 2)));
                        start = index;
                    }
                    run = current;
                }
                ids[index * 2] = (short) current.shapeId(shape);
                ids[index * 2 + 1] = (short) current.materialId(material);
            }
            final Sequence result;
            if (parts == null) result = run.sequence(ids);
            else {
                parts.add(run.sequence(Arrays.copyOfRange(ids, start * 2, ids.length)));
                result = new JoinedSequence(parts.toArray(Sequence[]::new));
            }
            if (closed) {
                current.seal();
                current = null;
            }
            return result;
        }

        void seal(final CanonicalRecords canonical) {
            synchronized (this) {
                closed = true;
                if (completed != null) {
                    for (final WeakReference<Page> reference : completed) {
                        final Page page = reference.get();
                        if (page != null) page.canonicalize(canonical);
                    }
                    completed = null;
                }
                if (current != null) {
                    current.seal();
                    current.canonicalize(canonical);
                }
                current = null;
            }
            // Publish closure before draining either index. An encoder already
            // inside its lock finishes before clear; later encoders never insert.
            // Drain one lock at a time, without holding the page monitor.
            synchronized (captureLock) {
                captures = null;
            }
            synchronized (materialLock) {
                capturedMaterials = null;
            }
        }
    }

    private static final class CanonicalRecords {
        private final Object2ObjectOpenHashMap<Shape, Shape> shapes = new Object2ObjectOpenHashMap<>();
        private final Object2ObjectOpenHashMap<BlockGeometryRecord.Columns, BlockGeometryRecord.Columns> columns = new Object2ObjectOpenHashMap<>();
        private final Object2ObjectOpenHashMap<Material, Material> materials = new Object2ObjectOpenHashMap<>();

        Shape shape(final Shape shape) {
            final Shape existing = shapes.putIfAbsent(shape, shape);
            return existing == null ? shape : existing;
        }

        Material material(final Material material) {
            final Material existing = materials.get(material);
            if (existing != null) return existing;
            final BlockGeometryRecord.Columns value = material.columns;
            final BlockGeometryRecord.Columns shared = value == null ? null : columns.putIfAbsent(value, value);
            final Material canonical = shared == null || shared == value ? material : new Material(shared, material);
            materials.put(canonical, canonical);
            return canonical;
        }
    }

    record Shape(BlockGeometryRecord.Shape shape, int stride, int position) {
        int[] decode(final BlockGeometryRecord.Columns columns) {
            return BlockGeometryRecord.decodeParts(shape, columns, stride, position);
        }
    }

    static final class Material {
        private final BlockGeometryRecord.Columns columns;
        private final TextureAtlasSprite sprite;
        private final int tint;
        private final byte face;
        private final boolean diffuse;
        private final VertexFormat format;
        private final int flags;

        Material(final BlockGeometryRecord.Columns columns, final BakedQuad quad) {
            this(columns, quad, ActiniumQuadFlags.get(quad));
        }

        private Material(final BlockGeometryRecord.Columns columns, final BakedQuad quad, final int flags) {
            this.columns = columns;
            sprite = quad.getSprite();
            tint = quad.getTintIndex();
            face = faceId(quad.getFace());
            diffuse = quad.shouldApplyDiffuseLighting();
            format = quad.getFormat();
            this.flags = flags;
        }

        private Material(final BlockGeometryRecord.Columns columns, final Material source) {
            this.columns = columns;
            sprite = source.sprite;
            tint = source.tint;
            face = source.face;
            diffuse = source.diffuse;
            format = source.format;
            flags = source.flags;
        }

        private static int hash(final BlockGeometryRecord.Columns columns, final TextureAtlasSprite sprite, final int tint,
                                final EnumFacing face, final boolean diffuse, final VertexFormat format, final int flags) {
            int result = System.identityHashCode(sprite) * 31 + tint;
            result = result * 31 + Objects.hashCode(face);
            result = result * 31 + Boolean.hashCode(diffuse);
            result = result * 31 + System.identityHashCode(format);
            result = result * 31 + Objects.hashCode(columns);
            return result * 31 + flags;
        }

        private static byte faceId(final EnumFacing face) {
            return face == null ? -1 : (byte) face.getIndex();
        }

        private EnumFacing facing() {
            return face < 0 ? null : EnumFacing.byIndex(face);
        }

        BakedQuad quad(final Shape shape) {
            final BakedQuad quad = new BakedQuad(shape.decode(columns), tint, facing(), sprite, diffuse, format);
            ActiniumQuadFlags.add(quad, flags);
            return quad;
        }

        private boolean matches(final BlockGeometryRecord.Columns columns, final BakedQuad quad, final int flags) {
            return sprite == quad.getSprite() && tint == quad.getTintIndex() && face == faceId(quad.getFace())
                && diffuse == quad.shouldApplyDiffuseLighting() && format == quad.getFormat()
                && this.flags == flags
                && Objects.equals(this.columns, columns);
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof Material that && sprite == that.sprite && tint == that.tint && face == that.face && diffuse == that.diffuse
                && format == that.format && flags == that.flags && Objects.equals(columns, that.columns);
        }

        @Override
        public int hashCode() {
            return hash(columns, sprite, tint, facing(), diffuse, format, flags);
        }
    }

    private static final class Page {
        private Shape[] shapes = new Shape[64];
        private Material[] materials = new Material[64];
        private Object2IntOpenHashMap<Shape> shapeIndex = new Object2IntOpenHashMap<>();
        private Object2IntOpenHashMap<Material> materialIndex = new Object2IntOpenHashMap<>();
        private Int2ObjectOpenHashMap<Sequence> sequences = new Int2ObjectOpenHashMap<>();
        private int shapeCount;
        private int materialCount;

        private Page() {
            shapeIndex.defaultReturnValue(-1);
            materialIndex.defaultReturnValue(-1);
        }

        boolean fits(final Shape shape, final Material material) {
            return (shapeCount < PAGE_SIZE || shapeIndex.containsKey(shape)) && (materialCount < PAGE_SIZE || materialIndex.containsKey(material));
        }

        int shapeId(final Shape shape) {
            final int existing = shapeIndex.getInt(shape);
            if (existing != -1) return existing;
            if (shapeCount == shapes.length) shapes = Arrays.copyOf(shapes, Math.min(PAGE_SIZE, shapeCount * 2));
            shapes[shapeCount] = shape;
            shapeIndex.put(shape, shapeCount);
            return shapeCount++;
        }

        int materialId(final Material material) {
            final int existing = materialIndex.getInt(material);
            if (existing != -1) return existing;
            if (materialCount == materials.length)
                materials = Arrays.copyOf(materials, Math.min(PAGE_SIZE, materialCount * 2));
            materials[materialCount] = material;
            materialIndex.put(material, materialCount);
            return materialCount++;
        }

        Sequence sequence(final short[] ids) {
            if (ids.length <= 4) {
                long packed = 0;
                for (int index = 0; index < ids.length; index++) packed |= (long) Short.toUnsignedInt(ids[index]) << index * 12;
                return sequence(packed, ids.length / 2);
            }
            // A very common sequence can be shared; the lookup itself remains bounded.
            if (sequences == null) return new PageSequence(this, ids);
            final int hash = Arrays.hashCode(ids);
            final Sequence existing = sequences.get(hash);
            if (existing instanceof PageSequence sequence && Arrays.equals(ids, sequence.ids)) return existing;
            final PageSequence candidate = new PageSequence(this, ids);
            if (sequences.size() < PAGE_SIZE) sequences.put(hash, candidate);
            return candidate;
        }

        Sequence sequence(final long ids, final int count) {
            final long packed = ids | (long) (count - 1) << 48;
            final int hash = PackedSequence.idsHash(packed);
            if (sequences != null) {
                final Sequence existing = sequences.get(hash);
                if (existing instanceof PackedSequence sequence && packed == sequence.ids) return existing;
            }
            final PackedSequence candidate = new PackedSequence(this, packed);
            if (sequences != null && sequences.size() < PAGE_SIZE) sequences.put(hash, candidate);
            return candidate;
        }

        void seal() {
            if (shapeIndex == null) return;
            if (shapes.length != shapeCount) shapes = Arrays.copyOf(shapes, shapeCount);
            if (materials.length != materialCount) materials = Arrays.copyOf(materials, materialCount);
            shapeIndex = null;
            materialIndex = null;
            sequences = null;
        }

        void canonicalize(final CanonicalRecords canonical) {
            for (int index = 0; index < shapes.length; index++) shapes[index] = canonical.shape(shapes[index]);
            for (int index = 0; index < materials.length; index++)
                materials[index] = canonical.material(materials[index]);
        }
    }

    /** One or two pairs of unsigned 12-bit page IDs fit in one word, including their count. */
    private record PackedSequence(Page page, long ids) implements Sequence {

        private static int idsHash(final long ids) {
            int hash = 1;
            for (int index = 0, n = ((int) (ids >>> 48) + 1) * 2; index < n; index++)
                hash = hash * 31 + (int) (ids >>> index * 12 & (PAGE_SIZE - 1));
            return hash;
        }

        @Override
        public int size() {
            return (int) (ids >>> 48) + 1;
        }

        @Override
        public BakedQuad quad(final int index) {
            Objects.checkIndex(index, size());
            final long pair = ids >>> index * 24;
            return page.materials[(int) (pair >>> 12 & (PAGE_SIZE - 1))].quad(page.shapes[(int) (pair & (PAGE_SIZE - 1))]);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(page) * 31 + idsHash(ids);
        }
    }

    private record PageSequence(Page page, short[] ids) implements Sequence {

        @Override
        public int size() {
            return ids.length / 2;
        }

        @Override
        public BakedQuad quad(final int index) {
            Objects.checkIndex(index, size());
            return page.materials[Short.toUnsignedInt(ids[index * 2 + 1])].quad(page.shapes[Short.toUnsignedInt(ids[index * 2])]);
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof PageSequence(Page page1, short[] ids1) && page == page1 && Arrays.equals(ids, ids1);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(page) * 31 + Arrays.hashCode(ids);
        }
    }

    private static final class JoinedSequence implements Sequence {
        private final Sequence[] parts;
        private final int size;

        private JoinedSequence(final Sequence[] parts) {
            this.parts = parts;
            int total = 0;
            for (final Sequence part : parts) total += part.size();
            size = total;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public BakedQuad quad(int index) {
            Objects.checkIndex(index, size);
            for (final Sequence part : parts) {
                if (index < part.size()) return part.quad(index);
                index -= part.size();
            }
            throw new AssertionError();
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof JoinedSequence that && Arrays.equals(parts, that.parts);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(parts);
        }
    }
}
