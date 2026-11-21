package github.kasuminova.novaeng.client.geometry.item;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.novaeng.client.geometry.CompactModel;
import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Generated item geometry; only public compatibility reads allocate the original Forge quads.
 */
public final class CompactItemModel implements IBakedModel, CompactModel {
    private final Storage storage;
    private final boolean gui;
    private final CompactItemModel guiModel;

    public CompactItemModel(final ItemContour[] contours, final TextureAtlasSprite[] sprites,
                            final TextureAtlasSprite particle, final ImmutableMap<TransformType, TRSRTransformation> transforms,
                            final ItemOverrideList overrides) {
        this(contours, sprites, particle, transforms, overrides, DefaultVertexFormats.ITEM);
    }

    public CompactItemModel(final ItemContour[] contours, final TextureAtlasSprite[] sprites,
                            final TextureAtlasSprite particle, final ImmutableMap<TransformType, TRSRTransformation> transforms,
                            final ItemOverrideList overrides, final VertexFormat format) {
        this(new Storage(contours.clone(), sprites.clone(), particle, transforms, overrides, format), false);
    }

    /** The private ItemLayerModel bake transfers its completed local arrays and never uses them again. */
    public static CompactItemModel fromOwnedLayers(final ItemContour[] contours, final TextureAtlasSprite[] sprites,
                                                   final TextureAtlasSprite particle,
                                                   final ImmutableMap<TransformType, TRSRTransformation> transforms,
                                                   final ItemOverrideList overrides, final VertexFormat format) {
        return new CompactItemModel(new Storage(contours, sprites, particle, transforms, overrides, format), false);
    }

    private CompactItemModel(final Storage storage, final boolean gui) {
        this.storage = storage;
        this.gui = gui;
        if (gui) guiModel = null;
        else {
            storage.owner = this;
            final TRSRTransformation transform = storage.transforms.get(TransformType.GUI);
            guiModel = transform == null || transform.isIdentity() ? new CompactItemModel(storage, true) : null;
        }
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public @NonNull TextureAtlasSprite getParticleTexture() {
        return storage.particle;
    }

    @Override
    public @NonNull ItemOverrideList getOverrides() {
        return storage.overrides;
    }

    @Override
    public @NonNull List<BakedQuad> getQuads(@Nullable final IBlockState state, @Nullable final EnumFacing face, final long seed) {
        if (face != null) return ObjectLists.emptyList();
        return storage.materialize().quads(gui);
    }

    @Override
    public List<BakedQuad> novaeng$readQuads(@Nullable final IBlockState state, @Nullable final EnumFacing face, final long seed) {
        if (face != null) return ObjectLists.emptyList();
        // ITEM readers copy packed vertices directly. Other formats need Forge's original format conversion.
        if (!novaeng$isItemFormat()) return getQuads(state, face, seed);
        final Geometry existing = storage.geometry;
        return existing instanceof PublishedGeometry published ? published.quads(gui) : new CursorList(storage, gui);
    }

    @Override
    public boolean novaeng$isGeometryExposed() {
        return storage.geometry instanceof PublishedGeometry;
    }

    public boolean novaeng$isItemFormat() {
        return storage.format.equals(DefaultVertexFormats.ITEM);
    }

    @Override
    public @NonNull Pair<? extends IBakedModel, Matrix4f> handlePerspective(final @NonNull TransformType type) {
        if (type == TransformType.GUI && guiModel != null) return guiModel.handlePerspective(type);
        if (gui && type != TransformType.GUI) return storage.owner.handlePerspective(type);
        return PerspectiveMapWrapper.handlePerspective(this, storage.transforms, type);
    }

    private interface Geometry {
        int size(boolean gui);
    }

    private record CompactGeometry(ItemContour[] contours, TextureAtlasSprite[] sprites,
                                   int[] ends) implements Geometry {
        @Override
        public int size(final boolean gui) {
            return gui ? contours.length : ends.length == 0 ? 0 : ends[ends.length - 1];
        }
    }

    private record PublishedGeometry(ObjectList<BakedQuad> quads,
                                     ObjectList<BakedQuad> guiQuads) implements Geometry {
        ObjectList<BakedQuad> quads(final boolean gui) {
            return gui ? guiQuads : quads;
        }

        @Override
        public int size(final boolean gui) {
            return quads(gui).size();
        }
    }

    private static final class Storage {
        private final TextureAtlasSprite particle;
        // Forge's PerspectiveMapWrapper.handlePerspective requires this exact Guava map type.
        private final ImmutableMap<TransformType, TRSRTransformation> transforms;
        private final ItemOverrideList overrides;
        private final VertexFormat format;
        private volatile Geometry geometry;
        private CompactItemModel owner;

        Storage(final ItemContour[] contours, final TextureAtlasSprite[] sprites, final TextureAtlasSprite particle,
                final ImmutableMap<TransformType, TRSRTransformation> transforms, final ItemOverrideList overrides,
                final VertexFormat format) {
            this.particle = particle;
            this.transforms = transforms;
            this.overrides = overrides;
            this.format = format;
            final int[] ends = new int[contours.length];
            int total = 0;
            for (int i = 0; i < contours.length; i++) ends[i] = total += contours[i].size() + 2;
            geometry = new CompactGeometry(contours, sprites, ends);
        }

        synchronized PublishedGeometry materialize() {
            if (geometry instanceof PublishedGeometry published) return published;
            final CompactGeometry compact = (CompactGeometry) geometry;
            final ObjectArrayList<BakedQuad> all = new ObjectArrayList<>(compact.size(false));
            final ObjectArrayList<BakedQuad> south = new ObjectArrayList<>(compact.size(true));
            final ItemQuadView cursor = new ItemQuadView(format);
            for (int layer = 0; layer < compact.contours.length; layer++) {
                for (int i = 0; i < compact.contours[layer].size() + 2; i++) {
                    cursor.decode(compact.contours[layer], compact.sprites[layer], layer, i);
                    final BakedQuad quad = cursor.materialize();
                    all.add(quad);
                    if (i == compact.contours[layer].size() + 1) south.add(quad);
                }
            }
            final PublishedGeometry published = new PublishedGeometry(ObjectLists.unmodifiable(all), ObjectLists.unmodifiable(south));
            // One volatile transition publishes both lists and releases all compact arrays together.
            geometry = published;
            return published;
        }
    }

    /**
     * One cursor belongs to one list, so obtaining another list cannot overwrite this list's current quad.
     */
    private static final class CursorList extends AbstractObjectList<BakedQuad> implements RandomAccess {
        private final Storage storage;
        private final boolean gui;
        private ItemQuadView cursor;

        CursorList(final Storage storage, final boolean gui) {
            this.storage = storage;
            this.gui = gui;
        }

        @Override
        public int size() {
            return storage.geometry.size(gui);
        }

        @Override
        public BakedQuad get(final int index) {
            // Retain one immutable state snapshot if a public read publishes while we decode.
            final Geometry geometry = storage.geometry;
            Objects.checkIndex(index, geometry.size(gui));
            // A compatibility read may have exposed mutable original arrays since this list was obtained.
            if (geometry instanceof PublishedGeometry published) return published.quads(gui).get(index);
            final CompactGeometry compact = (CompactGeometry) geometry;
            int layer = gui ? index : 0;
            if (!gui && index >= compact.ends[0]) {
                final int found = Arrays.binarySearch(compact.ends, 1, compact.ends.length, index + 1);
                layer = found >= 0 ? found : -found - 1;
            }
            final int local = gui ? compact.contours[layer].size() + 1 : index - (layer == 0 ? 0 : compact.ends[layer - 1]);
            if (cursor == null) cursor = new ItemQuadView(storage.format);
            cursor.decode(compact.contours[layer], compact.sprites[layer], layer, local);
            cursor.getVertexData(); // Cleanroom also exposes BakedQuad.vertexData directly.
            return cursor;
        }
    }
}
