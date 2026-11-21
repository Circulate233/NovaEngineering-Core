package github.kasuminova.novaeng.client.enderutilities;

import com.google.common.collect.ImmutableMap;
import dhj.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import fi.dy.masa.enderutilities.item.ItemNullifier;
import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * A resource-generation-local compositor. It owns baked data, never a ModelLoader, IModel, stack or world.
 */
public final class NullifierBakedModel implements IBakedModel {
    static final int MAX_CACHE_ENTRIES = 64;
    static final int MAX_CACHE_WORDS = 64 * 1024;
    static final int MAX_CACHE_QUADS = 2048;
    private static final EnumFacing[] FACES = EnumFacing.values();
    private static final ThreadLocal<int[]> RESOLUTION_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private final IBakedModel base;
    private final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> perspectives;
    private final TRSRTransformation placement;
    private final List<BakedQuad>[][] shells;
    private final IBakedModel[] empty = new IBakedModel[2];
    private final Entry[] cache = new Entry[MAX_CACHE_ENTRIES];
    private final ItemOverrideList overrides = new Overrides();
    private final ItemModelLookup lookup;
    private int next;
    private int words;
    private int quads;

    public NullifierBakedModel(final IBakedModel base, final IBakedModel locked,
                               final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> perspectives,
                               final TRSRTransformation placement) {
        this(base, locked, perspectives, placement,
            (stack, world, entity) -> Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack, world, entity));
    }

    @SuppressWarnings("unchecked")
    NullifierBakedModel(final IBakedModel base, final IBakedModel locked,
                        final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> perspectives,
                        final TRSRTransformation placement, final ItemModelLookup lookup) {
        this.base = base;
        this.perspectives = perspectives;
        this.placement = placement;
        this.lookup = lookup;
        shells = new List[2][7];
        for (int side = 0; side < 7; side++) {
            shells[0][side] = List.copyOf(base.getQuads(null, face(side), 0));
            shells[1][side] = join(shells[0][side], locked.getQuads(null, face(side), 0));
        }
        empty[0] = new Variant(shells[0]);
        empty[1] = new Variant(shells[1]);
    }

    private static EnumFacing face(final int index) {
        return index == 0 ? null : FACES[index - 1];
    }

    private static List<BakedQuad> join(final List<BakedQuad> first, final List<BakedQuad> second) {
        if (first.isEmpty()) return List.copyOf(second);
        if (second.isEmpty()) return first;
        if (first.size() == 1 && second.size() == 1) return List.of(first.getFirst(), second.getFirst());
        final BakedQuad[] result = new BakedQuad[first.size() + second.size()];
        int index = 0;
        for (final BakedQuad quad : first) result[index++] = Objects.requireNonNull(quad);
        for (final BakedQuad quad : second) result[index++] = Objects.requireNonNull(quad);
        return new ObjectImmutableList<>(result);
    }

    private static List<BakedQuad> joinOwned(final List<BakedQuad> first, final List<BakedQuad> second) {
        if (first.isEmpty()) return second;
        if (second.isEmpty()) return first;
        if (first.size() == 1 && second.size() == 1) return List.of(first.getFirst(), second.getFirst());
        return ObjectLists.unmodifiable(new JoinedQuads(first, second));
    }

    private static List<BakedQuad> freezeOwned(final ObjectArrayList<BakedQuad> quads) {
        if (quads == null) return ObjectLists.emptyList();
        if (quads.size() == 1) return List.of(quads.getFirst());
        if (quads.size() == 2) return List.of(quads.getFirst(), quads.get(1));
        return freezeOwnedArray(quads);
    }

    private static <T> List<T> freezeOwnedArray(final ObjectArrayList<T> quads) {
        quads.trim();
        // Keep T erased here: ObjectArrayList allocates Object[], not a concrete component array.
        // The local builder is discarded, so the immutable list takes its only backing array.
        return new ObjectImmutableList<>(quads.elements());
    }

    /** Both component lists have already been frozen by this compositor. */
    private static final class JoinedQuads extends AbstractObjectList<BakedQuad> implements RandomAccess {
        private final List<BakedQuad> first;
        private final List<BakedQuad> second;

        private JoinedQuads(final List<BakedQuad> first, final List<BakedQuad> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public BakedQuad get(final int index) {
            Objects.checkIndex(index, size());
            return index < first.size() ? first.get(index) : second.get(index - first.size());
        }

        @Override
        public int size() {
            return first.size() + second.size();
        }
    }

    IBakedModel resolveContained(final boolean locked, final ItemStack contained, @Nullable final World world,
                                 @Nullable final EntityLivingBase entity) {
        final int[] depth = RESOLUTION_DEPTH.get();
        if (depth[0] >= 16) return resolve(locked, null);
        depth[0]++;
        try {
            return resolve(locked, lookup.resolve(contained, world, entity));
        } finally {
            depth[0]--;
        }
    }

    synchronized IBakedModel resolve(final boolean locked, @Nullable final IBakedModel source) {
        if (source == null || source == this) return empty[locked ? 1 : 0];
        for (int index = 0; index < cache.length; index++) {
            final Entry entry = cache[index];
            if (entry == null) continue;
            final IBakedModel previous = entry.source.get();
            if (previous == null) evict(index);
            else if (previous == source) {
                if (entry.matches(source)) return entry.variant(locked);
                evict(index);
                break;
            }
        }
        final Entry entry = new Entry(source);
        if (entry.snapshots != null) {
            while (words + entry.words > MAX_CACHE_WORDS || quads + entry.quads > MAX_CACHE_QUADS) {
                evict(next);
                next = (next + 1) % cache.length;
            }
            evict(next);
            cache[next] = entry;
            words += entry.words;
            quads += entry.quads;
            next = (next + 1) % cache.length;
        }
        return entry.variant(locked);
    }

    private void evict(final int index) {
        final Entry entry = cache[index];
        if (entry != null) {
            words -= entry.words;
            quads -= entry.quads;
            cache[index] = null;
        }
    }

    @Override
    public @NonNull List<BakedQuad> getQuads(@Nullable final IBlockState state, @Nullable final EnumFacing side, final long seed) {
        return shells[0][side == null ? 0 : side.ordinal() + 1];
    }

    @Override
    public boolean isAmbientOcclusion() {
        return base.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return base.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return base.isBuiltInRenderer();
    }

    @Override
    public @NonNull TextureAtlasSprite getParticleTexture() {
        return base.getParticleTexture();
    }

    @Override
    public @NonNull ItemCameraTransforms getItemCameraTransforms() {
        return base.getItemCameraTransforms();
    }

    @Override
    public @NonNull ItemOverrideList getOverrides() {
        return overrides;
    }

    @Override
    public @NonNull Pair<? extends IBakedModel, Matrix4f> handlePerspective(final ItemCameraTransforms.@NonNull TransformType type) {
        return PerspectiveMapWrapper.handlePerspective(this, perspectives, type);
    }

    @FunctionalInterface
    interface ItemModelLookup {
        IBakedModel resolve(ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity);
    }

    private static final class QuadSnapshot {
        private final int[] words;
        private final VertexFormat format;
        private final TextureAtlasSprite sprite;
        private final EnumFacing face;
        private final int tint;
        private final boolean diffuse;
        private final int flags;

        private QuadSnapshot(final BakedQuad quad, final int[] snapshotWords, final VertexFormat format) {
            words = snapshotWords;
            this.format = format;
            sprite = quad.getSprite();
            face = quad.getFace();
            tint = quad.getTintIndex();
            diffuse = quad.shouldApplyDiffuseLighting();
            flags = quad instanceof BakedQuadView extension ? extension.getFlags() : 0;
        }

        private boolean matches(final BakedQuad quad) {
            return tint == quad.getTintIndex() && face == quad.getFace() && sprite == quad.getSprite()
                && diffuse == quad.shouldApplyDiffuseLighting() && format.equals(quad.getFormat())
                && flags == (quad instanceof BakedQuadView extension ? extension.getFlags() : 0)
                && Arrays.equals(words, quad.getVertexData());
        }
    }

    private final class Entry {
        private final WeakReference<IBakedModel> source;
        // Oversize models are still rendered, but never retain or keep constructing unused validation data.
        private QuadSnapshot[][] snapshots = new QuadSnapshot[7][];
        private final List<BakedQuad>[] transformed;
        private final IBakedModel[] variants = new IBakedModel[2];
        private int words;
        private int quads;

        @SuppressWarnings("unchecked")
        private Entry(final IBakedModel model) {
            source = new WeakReference<>(model);
            final ObjectArrayList<BakedQuad>[] buckets = new ObjectArrayList[7];
            VertexFormat firstFormat = null, firstFormatCopy = null;
            Reference2ObjectOpenHashMap<VertexFormat, VertexFormat> otherFormats = null;
            int[] previousWords = null;
            final NullifierQuadTransform.Scratch scratch = new NullifierQuadTransform.Scratch();
            for (int side = 0; side < 7; side++) {
                final List<BakedQuad> list = model.getQuads(null, face(side), 0);
                final int size = list.size();
                if (snapshots != null && size > MAX_CACHE_QUADS - quads) snapshots = null;
                if (snapshots != null) snapshots[side] = new QuadSnapshot[size];
                final int target = side == 0 ? 0 : placement.rotate(face(side)).ordinal() + 1;
                if (!list.isEmpty() && buckets[target] == null) buckets[target] = new ObjectArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    final BakedQuad quad = list.get(i);
                    final int[] sourceWords = quad.getVertexData();
                    if (snapshots != null && sourceWords.length > MAX_CACHE_WORDS - words) snapshots = null;
                    if (snapshots != null) {
                        final VertexFormat format = quad.getFormat();
                        final VertexFormat formatCopy;
                        if (firstFormat == null) {
                            firstFormat = format;
                            formatCopy = firstFormatCopy = new VertexFormat(format);
                        } else if (format == firstFormat) formatCopy = firstFormatCopy;
                        else {
                            if (otherFormats == null) otherFormats = new Reference2ObjectOpenHashMap<>(2);
                            VertexFormat saved = otherFormats.get(format);
                            if (saved == null) {
                                saved = new VertexFormat(format);
                                otherFormats.put(format, saved);
                            }
                            formatCopy = saved;
                        }
                        if (!Arrays.equals(previousWords, sourceWords)) previousWords = sourceWords.clone();
                        snapshots[side][i] = new QuadSnapshot(quad, previousWords, formatCopy);
                    }
                    buckets[target].add(scratch.transform(quad, placement, sourceWords));
                    words += sourceWords.length;
                    quads++;
                }
            }
            transformed = new List[7];
            for (int side = 0; side < 7; side++) transformed[side] = freezeOwned(buckets[side]);
        }

        private boolean matches(final IBakedModel model) {
            for (int side = 0; side < 7; side++) {
                final List<BakedQuad> list = model.getQuads(null, face(side), 0);
                final QuadSnapshot[] previous = snapshots[side];
                if (previous.length != list.size()) return false;
                for (int i = 0; i < previous.length; i++) if (!previous[i].matches(list.get(i))) return false;
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        private IBakedModel variant(final boolean locked) {
            final int index = locked ? 1 : 0;
            if (variants[index] == null) {
                final List<BakedQuad>[] lists = new List[7];
                for (int side = 0; side < 7; side++) lists[side] = joinOwned(shells[index][side], transformed[side]);
                variants[index] = new Variant(lists);
            }
            return variants[index];
        }
    }

    private final class Overrides extends ItemOverrideList {
        @Override
        public @NonNull IBakedModel handleItemState(final @NonNull IBakedModel original, final @NonNull ItemStack stack, @Nullable final World world,
                                                    @Nullable final EntityLivingBase entity) {
            final boolean locked = !ItemNullifier.isNullifierEnabled(stack);
            final ItemStack selected = ItemNullifier.getSelectedStack(stack);
            return selected.isEmpty() ? resolve(locked, null) : resolveContained(locked, selected, world, entity);
        }
    }

    private final class Variant implements IBakedModel {
        private final List<BakedQuad>[] lists;

        private Variant(final List<BakedQuad>[] lists) {
            this.lists = lists;
        }

        @Override
        public @NonNull List<BakedQuad> getQuads(@Nullable final IBlockState state, @Nullable final EnumFacing side, final long seed) {
            return lists[side == null ? 0 : side.ordinal() + 1];
        }

        @Override
        public boolean isAmbientOcclusion() {
            return base.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return base.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return base.isBuiltInRenderer();
        }

        @Override
        public @NonNull TextureAtlasSprite getParticleTexture() {
            return base.getParticleTexture();
        }

        @Override
        public @NonNull ItemCameraTransforms getItemCameraTransforms() {
            return base.getItemCameraTransforms();
        }

        @Override
        public @NonNull ItemOverrideList getOverrides() {
            return overrides;
        }

        @Override
        public @NonNull Pair<? extends IBakedModel, Matrix4f> handlePerspective(final ItemCameraTransforms.@NonNull TransformType type) {
            return PerspectiveMapWrapper.handlePerspective(this, perspectives, type);
        }
    }
}
