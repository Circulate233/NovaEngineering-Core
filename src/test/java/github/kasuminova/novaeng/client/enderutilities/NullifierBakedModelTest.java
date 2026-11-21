package github.kasuminova.novaeng.client.enderutilities;

import com.dhj.actinium.mixin.vintage.core.terrain.MixinBakedQuad;
import com.google.common.collect.ImmutableMap;
import com.sun.management.ThreadMXBean;
import dhj.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import github.kasuminova.novaeng.testutil.ForgeReflectionEnvironment;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NullifierBakedModelTest {
    private static final Sprite SPRITE = new Sprite();

    @BeforeAll
    static void prepareForge() {
        ForgeReflectionEnvironment.initialize();
    }

    private static void inspect(final Object object, final IdentityHashMap<Object, Boolean> seen, final Object... forbidden) throws Exception {
        if (object == null || seen.put(object, true) != null) return;
        for (final Object value : forbidden) assertNotSame(value, object);
        assertFalse(object instanceof IModel || object instanceof ModelLoader || object instanceof ItemStack);
        if (object instanceof Object[] array) {
            for (final Object child : array) inspect(child, seen, forbidden);
        } else if (object instanceof Iterable<?> items) {
            for (final Object child : items) inspect(child, seen, forbidden);
        } else if (object.getClass().getName().startsWith(NullifierBakedModel.class.getName())) {
            for (final var field : object.getClass().getDeclaredFields())
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    inspect(field.get(object), seen, forbidden);
                }
        }
    }

    private static void assertCacheBounds(final NullifierBakedModel root) throws Exception {
        assertTrue((int) field(root, "words") <= NullifierBakedModel.MAX_CACHE_WORDS);
        assertTrue((int) field(root, "quads") <= NullifierBakedModel.MAX_CACHE_QUADS);
        final Object[] entries = (Object[]) field(root, "cache");
        assertTrue(Arrays.stream(entries).filter(Objects::nonNull).count() <= NullifierBakedModel.MAX_CACHE_ENTRIES);
    }

    private static Object field(final Object object, final String name) throws Exception {
        final var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static NullifierBakedModel root(final IBakedModel frame, final IBakedModel overlay) {
        return new NullifierBakedModel(frame, overlay, ImmutableMap.of(), NullifierQuadTransform.placement(TRSRTransformation.identity()));
    }

    private static BakedQuad quad(final float x) {
        final int[] words = new int[DefaultVertexFormats.ITEM.getIntegerSize() * 4];
        for (int v = 0; v < 4; v++) {
            final int start = v * DefaultVertexFormats.ITEM.getIntegerSize();
            words[start] = Float.floatToRawIntBits(x + (v % 2) * 0.1F);
            words[start + 1] = Float.floatToRawIntBits(v / 2F);
            words[start + 2] = Float.floatToRawIntBits(0.3F);
            words[start + 3] = 0xBBAA9988;
            words[start + 4] = Float.floatToRawIntBits(0.23F);
            words[start + 5] = Float.floatToRawIntBits(0.79F);
            words[start + 6] = 0x2A007F00;
        }
        return new BakedQuad(words, 3, EnumFacing.UP, SPRITE, true, DefaultVertexFormats.ITEM);
    }

    @Test
    void emptySelectedAndLockedShellsComposeWithoutMutatingTheirInputs() {
        final BakedQuad frame = quad(0.05F), overlay = quad(0.15F), item = quad(0.75F);
        final int[] original = item.getVertexData().clone();
        final Stub source = new Stub(item);
        final NullifierBakedModel root = root(new Stub(frame), new Stub(overlay));
        assertEquals(List.of(frame), root.resolve(false, null).getQuads(null, null, 0));
        assertEquals(List.of(frame, overlay), root.resolve(true, null).getQuads(null, null, 0));
        final IBakedModel unlocked = root.resolve(false, source);
        final IBakedModel locked = root.resolve(true, source);
        assertSame(unlocked, root.resolve(false, source));
        assertSame(locked, root.resolve(true, source));
        assertEquals(2, unlocked.getQuads(null, null, 0).size());
        assertEquals(3, locked.getQuads(null, null, 0).size());
        assertSame(frame, locked.getQuads(null, null, 0).get(0));
        assertSame(overlay, locked.getQuads(null, null, 0).get(1));
        final BakedQuad display = unlocked.getQuads(null, null, 0).get(1);
        assertNotSame(item, display);
        assertSame(display, locked.getQuads(null, null, 0).get(2));
        assertArrayEquals(original, item.getVertexData());
        assertEquals(item.getTintIndex(), display.getTintIndex());
        assertSame(item.getSprite(), display.getSprite());
        assertEquals(item.shouldApplyDiffuseLighting(), display.shouldApplyDiffuseLighting());
        assertThrows(UnsupportedOperationException.class, () -> unlocked.getQuads(null, null, 0).clear());
    }

    @Test
    void transformMatchesActualForgeItemLayerBakingAndPreservesEveryOtherAttribute() {
        final TRSRTransformation placement = NullifierQuadTransform.placement(TRSRTransformation.identity());
        final List<BakedQuad> before = ItemLayerModel.getQuadsForSprite(3, SPRITE, DefaultVertexFormats.ITEM, Optional.empty());
        final List<BakedQuad> expected = ItemLayerModel.getQuadsForSprite(3, SPRITE, DefaultVertexFormats.ITEM, Optional.of(placement));
        assertEquals(before.size(), expected.size());
        final NullifierQuadTransform.Scratch scratch = new NullifierQuadTransform.Scratch();
        for (int i = 0; i < before.size(); i++) {
            final BakedQuad actual = scratch.transform(before.get(i), placement, before.get(i).getVertexData());
            assertArrayEquals(NullifierQuadTransform.transform(before.get(i), placement).getVertexData(), actual.getVertexData());
            assertEquals(placement.rotate(before.get(i).getFace()), actual.getFace());
            final VertexFormat format = actual.getFormat();
            final float[] left = new float[4], right = new float[4];
            for (int vertex = 0; vertex < 4; vertex++) {
                for (int element = 0; element < format.getElementCount(); element++) {
                    LightUtil.unpack(expected.get(i).getVertexData(), left, format, vertex, element);
                    LightUtil.unpack(actual.getVertexData(), right, format, vertex, element);
                    assertArrayEquals(left, right, 0.000002F, "Forge layer vertex " + vertex + " element " + element);
                }
            }
        }
        final BakedQuad input = quad(0.4F);
        final BakedQuad output = NullifierQuadTransform.transform(input, placement);
        final VertexFormat format = input.getFormat();
        final float[] left = new float[4], right = new float[4];
        for (int e = 0; e < format.getElementCount(); e++) {
            final var usage = format.getElement(e).getUsage();
            if (usage == VertexFormatElement.EnumUsage.POSITION || usage == VertexFormatElement.EnumUsage.NORMAL)
                continue;
            for (int v = 0; v < 4; v++) {
                LightUtil.unpack(input.getVertexData(), left, format, v, e);
                LightUtil.unpack(output.getVertexData(), right, format, v, e);
                assertArrayEquals(left, right, 0F, "UV/color/padding must remain unchanged");
            }
        }
    }

    @Test
    void transformedQuadRecomputesActualActiniumGeometryFlagsAndRetainsProducerFlags() {
        final int[] words = new int[28];
        final int[] corners = {0, 1, 3, 2};
        for (int v = 0; v < 4; v++) {
            words[v * 7] = Float.floatToRawIntBits(corners[v] & 1);
            words[v * 7 + 1] = Float.floatToRawIntBits((corners[v] >>> 1) & 1);
            words[v * 7 + 2] = Float.floatToRawIntBits(1F);
            words[v * 7 + 3] = -1;
            words[v * 7 + 6] = 0x007F0000;
        }
        final BakedQuad source = new BakedQuad(words, -1, EnumFacing.SOUTH, SPRITE, true, DefaultVertexFormats.ITEM);
        final int producer = ModelQuadFlags.IS_VANILLA_SHADED | ModelQuadFlags.IS_TRUSTED_SPRITE | 0x4000;
        final ActiniumView sourceView = new ActiniumView(source);
        sourceView.addFlags(producer);
        final int originalFlags = sourceView.getFlags();
        assertNotEquals(0, originalFlags & ModelQuadFlags.IS_ALIGNED);
        final BakedQuad transformed = NullifierQuadTransform.transform(source, NullifierQuadTransform.placement(TRSRTransformation.identity()));
        final ActiniumView fresh = new ActiniumView(transformed);
        fresh.addFlags(producer);
        final ActiniumView restored = new ActiniumView(transformed);
        restored.addFlags(NullifierQuadTransform.producerFlags(originalFlags));
        assertEquals(producer, NullifierQuadTransform.producerFlags(originalFlags));
        assertEquals(fresh.getFlags(), restored.getFlags());
        assertEquals(0, restored.getFlags() & ModelQuadFlags.IS_ALIGNED);
        assertNotEquals(0, restored.getFlags() & ModelQuadFlags.IS_PARTIAL);
    }

    @Test
    void cullBucketsRotateAndSameObjectDynamicGeometryIsNotFrozen() {
        final BakedQuad north = quad(0.2F);
        final Stub source = new Stub();
        source.sides[EnumFacing.NORTH.ordinal() + 1] = List.of(north);
        final NullifierBakedModel root = root(new Stub(), new Stub());
        final IBakedModel first = root.resolve(false, source);
        final EnumFacing rotated = NullifierQuadTransform.placement(TRSRTransformation.identity()).rotate(EnumFacing.NORTH);
        assertEquals(1, first.getQuads(null, rotated, 0).size());
        assertTrue(first.getQuads(null, EnumFacing.NORTH, 0).isEmpty());
        north.getVertexData()[0] = Float.floatToRawIntBits(0.9F);
        final IBakedModel changed = root.resolve(false, source);
        assertNotSame(first, changed, "The same baked model may change its vertices");
        final int previousHash = Arrays.hashCode(north.getVertexData());
        north.getVertexData()[4]++;
        north.getVertexData()[5] -= 31;
        assertEquals(previousHash, Arrays.hashCode(north.getVertexData()));
        assertNotSame(changed, root.resolve(false, source), "Equal hashes cannot hide changed UV data");
        source.sides[EnumFacing.NORTH.ordinal() + 1] = List.of(quad(0.6F), quad(0.8F));
        assertEquals(2, root.resolve(false, source).getQuads(null, rotated, 0).size());
        source.sides[EnumFacing.NORTH.ordinal() + 1] = List.of();
        assertTrue(root.resolve(false, source).getQuads(null, rotated, 0).isEmpty());
    }

    @Test
    void runtimeLookupRunsForEachSelectedStackAndExceptionsAndRecursionDoNotPoisonIt() {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<IBakedModel> selected = new AtomicReference<>(new Stub(quad(0.2F)));
        final NullifierBakedModel root = new NullifierBakedModel(new Stub(), new Stub(), ImmutableMap.of(),
            NullifierQuadTransform.placement(TRSRTransformation.identity()), (stack, world, entity) -> {
            calls.incrementAndGet();
            if (calls.get() == 1) throw new IllegalStateException("probe");
            return selected.get();
        });
        // The override handler filters empty slots. This lower-level probe only needs a stack identity.
        final ItemStack stack = ItemStack.EMPTY;
        assertThrows(IllegalStateException.class, () -> root.resolveContained(false, stack, null, null));
        final IBakedModel first = root.resolveContained(false, stack, null, null);
        assertSame(first, root.resolveContained(false, stack, null, null));
        assertEquals(3, calls.get(), "Do not bypass NBT/world/entity-sensitive item overrides on cache hits");
        selected.set(new Stub(quad(0.8F)));
        final IBakedModel second = root.resolveContained(false, stack, null, null);
        assertNotSame(first, second);
        assertEquals(0, root.resolve(false, null).getQuads(null, null, 0).size());
        final NullifierBakedModel[] nested = new NullifierBakedModel[1];
        nested[0] = new NullifierBakedModel(new Stub(), new Stub(), ImmutableMap.of(), TRSRTransformation.identity(),
            (contained, world, entity) -> nested[0].resolveContained(false, contained, world, entity));
        assertNotNull(nested[0].resolveContained(false, stack, null, null), "Pathological nested Nullifiers must terminate");
        assertSame(second, root.resolveContained(false, stack, null, null));
    }

    @Test
    void cacheIsBoundedByEntriesAndGeometryAndDeadSourcesCanBeReleased() throws Exception {
        final NullifierBakedModel root = root(new Stub(), new Stub());
        final List<Stub> alive = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            final Stub source = new Stub(quad(i));
            alive.add(source);
            root.resolve((i & 1) == 0, source);
            assertCacheBounds(root);
        }
        final Object[] entries = (Object[]) field(root, "cache");
        int retained = 0;
        for (final Object entry : entries)
            if (entry != null) {
                retained++;
                assertInstanceOf(WeakReference.class, field(entry, "source")).clear();
            }
        assertEquals(NullifierBakedModel.MAX_CACHE_ENTRIES, retained);
        root.resolve(false, new Stub(quad(0)));
        assertEquals(1, Arrays.stream(entries).filter(Objects::nonNull).count());
        final BakedQuad[] lots = new BakedQuad[1024];
        Arrays.fill(lots, quad(0.5F));
        for (int i = 0; i < 6; i++) {
            final Stub source = new Stub(lots);
            alive.add(source);
            root.resolve(false, source);
            assertCacheBounds(root);
        }
        final BakedQuad[] oversized = new BakedQuad[NullifierBakedModel.MAX_CACHE_QUADS + 1];
        Arrays.fill(oversized, quad(0.7F));
        final int cachedWords = (int) field(root, "words");
        assertEquals(oversized.length, root.resolve(false, new Stub(oversized)).getQuads(null, null, 0).size());
        assertEquals(cachedWords, field(root, "words"), "An oversize model remains renderable without growing the cache");
    }

    @Test
    void coldCaptureReadsProducerVertexStorageOnlyOnce() {
        final BakedQuad original = quad(0.3F);
        final AtomicInteger reads = new AtomicInteger();
        final BakedQuad counted = new BakedQuad(original.getVertexData(), original.getTintIndex(), original.getFace(), SPRITE,
            original.shouldApplyDiffuseLighting(), original.getFormat()) {
            @Override
            public int[] getVertexData() {
                reads.incrementAndGet();
                return super.getVertexData();
            }
        };
        final NullifierBakedModel root = root(new Stub(), new Stub());
        final Stub source = new Stub(counted);
        root.resolve(false, source);
        assertEquals(1, reads.get(), "Some compact/custom quads allocate when their vertex data is read");
        root.resolve(false, source);
        assertEquals(2, reads.get());
    }

    @Test
    void oversizeCapturesDiscardValidationDataAndStillTransformEachSourceOnlyOnce() throws Exception {
        final NullifierBakedModel root = root(new Stub(), new Stub());
        final BakedQuad original = quad(0.4F);
        final AtomicInteger reads = new AtomicInteger();
        final BakedQuad counted = new BakedQuad(original.getVertexData(), original.getTintIndex(), original.getFace(), SPRITE,
            original.shouldApplyDiffuseLighting(), original.getFormat()) {
            @Override
            public int[] getVertexData() {
                reads.incrementAndGet();
                return super.getVertexData();
            }
        };
        final BakedQuad[] lots = new BakedQuad[NullifierBakedModel.MAX_CACHE_QUADS + 1];
        Arrays.fill(lots, counted);
        final Class<?> entryType = Arrays.stream(NullifierBakedModel.class.getDeclaredClasses())
            .filter(type -> type.getSimpleName().equals("Entry")).findFirst().orElseThrow();
        final var constructor = entryType.getDeclaredConstructor(NullifierBakedModel.class, IBakedModel.class);
        constructor.setAccessible(true);
        final Object largeEntry = constructor.newInstance(root, new Stub(lots));
        assertNull(field(largeEntry, "snapshots"));
        assertEquals(lots.length, reads.get(), "Oversize rendering must not reread producers to preflight cache eligibility");
        final List<?>[] transformed = (List<?>[]) field(largeEntry, "transformed");
        assertEquals(lots.length, transformed[0].size());
        assertArrayEquals(NullifierQuadTransform.transform(original, NullifierQuadTransform.placement(TRSRTransformation.identity())).getVertexData(),
            ((BakedQuad) transformed[0].getLast()).getVertexData());

        final int[] longWords = Arrays.copyOf(original.getVertexData(), NullifierBakedModel.MAX_CACHE_WORDS + 1);
        longWords[longWords.length - 1] = 0x12345678;
        final BakedQuad longQuad = new BakedQuad(longWords, 3, EnumFacing.UP, SPRITE, true, DefaultVertexFormats.ITEM);
        final Object longEntry = constructor.newInstance(root, new Stub(longQuad));
        assertNull(field(longEntry, "snapshots"));
        final List<?>[] longResult = (List<?>[]) field(longEntry, "transformed");
        final int[] output = ((BakedQuad) longResult[0].getFirst()).getVertexData();
        assertNotSame(longWords, output);
        assertEquals(0x12345678, output[output.length - 1]);
        assertEquals(longWords.length, output.length);
    }

    @Test
    void equalPrivateInputSnapshotsShareStorageWhileSourceAndPublishedOutputArraysRemainIndependent() throws Exception {
        final BakedQuad first = quad(0.3F), equalButSeparate = quad(0.3F);
        final int[] expected = first.getVertexData().clone();
        final Stub source = new Stub(first, first, equalButSeparate);
        final NullifierBakedModel root = root(new Stub(), new Stub());
        final IBakedModel published = root.resolve(false, source);
        final Object entry = Arrays.stream((Object[]) field(root, "cache")).filter(Objects::nonNull).findFirst().orElseThrow();
        final Object[] snapshots = ((Object[][]) field(entry, "snapshots"))[0];
        final int[] privateWords = (int[]) field(snapshots[0], "words");
        assertSame(privateWords, field(snapshots[1], "words"));
        assertSame(privateWords, field(snapshots[2], "words"));
        assertNotSame(first.getVertexData(), privateWords);
        assertNotSame(equalButSeparate.getVertexData(), privateWords);
        final List<BakedQuad> transformed = published.getQuads(null, null, 0);
        assertNotSame(transformed.get(0).getVertexData(), transformed.get(1).getVertexData());
        assertNotSame(transformed.get(0).getVertexData(), transformed.get(2).getVertexData());
        final int secondOutput = transformed.get(1).getVertexData()[0];
        transformed.get(0).getVertexData()[0] = 99;
        assertEquals(secondOutput, transformed.get(1).getVertexData()[0]);
        assertArrayEquals(expected, privateWords);
        first.getVertexData()[4]++;
        assertArrayEquals(expected, privateWords);
        assertArrayEquals(expected, equalButSeparate.getVertexData());
        assertNotSame(published, root.resolve(false, source));
    }

    @Test
    void unchangedModelHitsDoNotAllocateNewGeometryKeysOrLists() {
        final NullifierBakedModel root = root(new Stub(), new Stub());
        final Stub source = new Stub(quad(0.4F));
        final IBakedModel expected = root.resolve(false, source);
        final var bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) return;
        bean.setThreadAllocatedMemoryEnabled(true);
        for (int i = 0; i < 20000; i++) root.resolve(false, source);
        long least = Long.MAX_VALUE;
        for (int trial = 0; trial < 3; trial++) {
            final long before = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
            for (int i = 0; i < 5000; i++) assertSame(expected, root.resolve(false, source));
            least = Math.min(least, bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - before);
        }
        assertTrue(least < 4096, "Cache hits allocated " + least + " bytes for 5,000 resolutions");
    }

    @Test
    void factoryDoesNotRetainUnbakedInputsStateOrTextureGetterAndReloadsAreIndependent() throws Exception {
        final Stub frame = new Stub(quad(0.1F)), overlay = new Stub(quad(0.2F));
        final AtomicInteger bakes = new AtomicInteger();
        final IModel unbaked = new IModel() {
            @Override
            public IModelState getDefaultState() {
                return TRSRTransformation.identity();
            }

            @Override
            public IBakedModel bake(final IModelState state, final VertexFormat format,
                                    final Function<ResourceLocation, TextureAtlasSprite> getter) {
                return bakes.getAndIncrement() % 2 == 0 ? frame : overlay;
            }
        };
        final IModelState state = part -> part.isEmpty() ? Optional.of(TRSRTransformation.identity()) : Optional.empty();
        final Function<ResourceLocation, TextureAtlasSprite> getter = location -> SPRITE;
        final NullifierBakedModel first = (NullifierBakedModel) NullifierModelFactory.bake(unbaked, unbaked, state, DefaultVertexFormats.ITEM, getter);
        final NullifierBakedModel second = (NullifierBakedModel) NullifierModelFactory.bake(unbaked, unbaked, state, DefaultVertexFormats.ITEM, getter);
        assertEquals(4, bakes.get());
        assertNotSame(first, second);
        assertNotSame(first.resolve(false, null), second.resolve(false, null));
        inspect(first, new IdentityHashMap<>(), unbaked, state, getter);
    }

    private static final class Sprite extends TextureAtlasSprite {
        private Sprite() {
            super("nullifier-test");
            setIconWidth(2);
            setIconHeight(2);
            initSprite(256, 256, 0, 0, false);
            setFramesTextureData(List.<int[][]>of(new int[][]{{-1, -1, -1, -1}}));
        }
    }

    private static final class ActiniumView extends MixinBakedQuad {
        private final BakedQuad source;

        private ActiniumView(final BakedQuad source) {
            this.source = source;
            face = source.getFace();
            sprite = source.getSprite();
            applyDiffuseLighting = source.shouldApplyDiffuseLighting();
        }

        @Override
        public int[] getVertexData() {
            return source.getVertexData();
        }

        @Override
        public VertexFormat getFormat() {
            return source.getFormat();
        }
    }

    private static final class Stub implements IBakedModel {
        private final List<BakedQuad>[] sides;

        @SuppressWarnings("unchecked")
        private Stub(final BakedQuad... quads) {
            sides = new List[7];
            Arrays.fill(sides, List.of());
            sides[0] = List.of(quads);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable final IBlockState state, @Nullable final EnumFacing side, final long seed) {
            return sides[side == null ? 0 : side.ordinal() + 1];
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
        public TextureAtlasSprite getParticleTexture() {
            return SPRITE;
        }

        @Override
        public ItemCameraTransforms getItemCameraTransforms() {
            return ItemCameraTransforms.DEFAULT;
        }

        @Override
        public ItemOverrideList getOverrides() {
            return ItemOverrideList.NONE;
        }
    }
}
