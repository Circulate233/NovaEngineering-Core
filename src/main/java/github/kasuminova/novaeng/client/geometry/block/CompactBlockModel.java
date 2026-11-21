package github.kasuminova.novaeng.client.geometry.block;

import github.kasuminova.novaeng.client.geometry.CompactModel;
import github.kasuminova.novaeng.client.geometry.TrustedGeometryList;
import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Only constructed at the owned vanilla bakery boundary; never snapshots arbitrary model results.
 */
public final class CompactBlockModel implements IBakedModel, CompactModel {
    private final boolean ambientOcclusion;
    private final boolean gui3d;
    private final TextureAtlasSprite particle;
    private final ItemCameraTransforms transforms;
    private final ItemOverrideList overrides;
    private volatile BlockGeometryPages.Sequence[] storage;
    private volatile List<BakedQuad>[] exposed;

    private CompactBlockModel(final SimpleBakedModel source) {
        ambientOcclusion = source.isAmbientOcclusion();
        gui3d = source.isGui3d();
        particle = source.getParticleTexture();
        transforms = source.getItemCameraTransforms();
        overrides = source.getOverrides();
    }

    /**
     * The pack's VanillaModelWrapper builder transfers fresh, distinct quads, arrays and cull lists here.
     */
    public static CompactBlockModel fromVanillaBake(final SimpleBakedModel source) {
        BlockGeometryPages pages = BlockGeometryPages.active();
        final boolean independent = pages == null;
        if (independent) pages = new BlockGeometryPages(1);
        final CompactBlockModel model = new CompactBlockModel(source);
        model.storage = capture(source, pages);
        if (independent) pages.seal();
        return model;
    }

    private static BlockGeometryPages.Sequence[] capture(final SimpleBakedModel source, final BlockGeometryPages pages) {
        final BlockGeometryPages.Sequence[] sequences = new BlockGeometryPages.Sequence[7];
        VertexFormat lastFormat = null;
        int position = -1;
        for (int index = 0; index < 7; index++) {
            final List<BakedQuad> quads = source.getQuads(null, index == 6 ? null : EnumFacing.byIndex(index), 0L);
            final int count = quads.size();
            if (count == 0) {
                sequences[index] = BlockGeometryPages.EMPTY;
                continue;
            }
            final BlockGeometryPages.Shape[] shapes = count > 2 ? new BlockGeometryPages.Shape[count] : null;
            final BlockGeometryPages.Material[] materials = count > 2 ? new BlockGeometryPages.Material[count] : null;
            BlockGeometryPages.Shape firstShape = null, secondShape = null;
            BlockGeometryPages.Material firstMaterial = null, secondMaterial = null;
            for (int face = 0; face < count; face++) {
                final BakedQuad quad = quads.get(face);
                final VertexFormat format = quad.getFormat();
                if (format != lastFormat) {
                    position = positionOffset(format);
                    lastFormat = format;
                }
                // AE2's UVL bakery supplies lazy UnpackedBakedQuads; their field is still zero until packed.
                final BlockGeometryRecord captured = pages.capture(quad.getVertexData(), format.getIntegerSize(), position);
                final BlockGeometryPages.Shape shape = captured.pageShape();
                final BlockGeometryPages.Material material = pages.material(captured.material, quad);
                if (shapes != null) {
                    shapes[face] = shape;
                    materials[face] = material;
                } else if (face == 0) {
                    firstShape = shape;
                    firstMaterial = material;
                } else {
                    secondShape = shape;
                    secondMaterial = material;
                }
            }
            sequences[index] = shapes == null ? pages.bind(firstShape, firstMaterial, secondShape, secondMaterial)
                : pages.bind(shapes, materials, 0, count);
        }
        return sequences;
    }

    private static int positionOffset(final VertexFormat format) {
        if (format.getSize() % 4 == 0) for (int element = 0; element < format.getElementCount(); element++) {
            final VertexFormatElement value = format.getElement(element);
            if (value.getUsage() == VertexFormatElement.EnumUsage.POSITION && value.getType() == VertexFormatElement.EnumType.FLOAT
                && value.getElementCount() >= 3 && format.getOffset(element) % 4 == 0)
                return format.getOffset(element) / 4;
        }
        return -1;
    }

    private static int index(final EnumFacing face) {
        return face == null ? 6 : face.getIndex();
    }

    @SuppressWarnings("unchecked")
    private static List<BakedQuad>[] newLists() {
        return (List<BakedQuad>[]) new List<?>[7];
    }

    @Override
    public @NonNull List<BakedQuad> getQuads(final IBlockState state, final EnumFacing side, final long rand) {
        return expose()[index(side)];
    }

    @Override
    public List<BakedQuad> novaeng$readQuads(final IBlockState state, final EnumFacing side, final long seed) {
        final List<BakedQuad>[] published = exposed;
        if (published != null) return published[index(side)];
        final BlockGeometryPages.Sequence[] snapshot = storage;
        return snapshot == null ? exposed[index(side)] : new ReadView(this, snapshot, index(side));
    }

    @Override
    public boolean novaeng$isGeometryExposed() {
        return exposed != null;
    }

    private synchronized List<BakedQuad>[] expose() {
        if (exposed != null) return exposed;
        final BlockGeometryPages.Sequence[] snapshot = storage;
        final List<BakedQuad>[] result = newLists();
        for (int side = 0; side < 7; side++) {
            final BlockGeometryPages.Sequence sequence = snapshot[side];
            result[side] = new ObjectArrayList<>(sequence.size());
            for (int index = 0; index < sequence.size(); index++) result[side].add(sequence.quad(index));
        }
        return publish(result);
    }

    private List<BakedQuad>[] publish(final List<BakedQuad>[] lists) {
        exposed = lists;
        storage = null;
        return lists;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public boolean isGui3d() {
        return gui3d;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public @NonNull TextureAtlasSprite getParticleTexture() {
        return particle;
    }

    @Override
    public @NonNull ItemCameraTransforms getItemCameraTransforms() {
        return transforms;
    }

    @Override
    public @NonNull ItemOverrideList getOverrides() {
        return overrides;
    }

    /**
     * One render pins its storage snapshot and decodes each face once; the model retains no decoded cache.
     */
    private static final class ReadView extends AbstractObjectList<BakedQuad> implements RandomAccess, TrustedGeometryList {
        private final CompactBlockModel owner;
        private final BlockGeometryPages.Sequence[] snapshot;
        private final int side;
        private BakedQuad single;
        private BakedQuad[] decoded;

        private ReadView(final CompactBlockModel owner, final BlockGeometryPages.Sequence[] snapshot, final int side) {
            this.owner = owner;
            this.snapshot = snapshot;
            this.side = side;
        }

        @Override
        public BakedQuad get(final int index) {
            final List<BakedQuad>[] exposed = owner.exposed;
            if (exposed != null) return exposed[side].get(index);
            final BlockGeometryPages.Sequence sequence = snapshot[side];
            Objects.checkIndex(index, sequence.size());
            if (sequence.size() == 1) {
                if (single == null) single = sequence.quad(index);
                return single;
            }
            if (decoded == null) decoded = new BakedQuad[sequence.size()];
            BakedQuad quad = decoded[index];
            if (quad == null) decoded[index] = quad = sequence.quad(index);
            return quad;
        }

        @Override
        public int size() {
            final List<BakedQuad>[] exposed = owner.exposed;
            return exposed == null ? snapshot[side].size() : exposed[side].size();
        }
    }
}
