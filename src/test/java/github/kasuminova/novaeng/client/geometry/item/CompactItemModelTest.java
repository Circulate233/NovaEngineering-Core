package github.kasuminova.novaeng.client.geometry.item;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import github.kasuminova.novaeng.mixin.geometry.actinium.MixinGeometryDisplayLists;
import github.kasuminova.novaeng.mixin.geometry.item.MixinItemLayerModel;
import github.kasuminova.novaeng.testutil.ForgeReflectionEnvironment;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.Attributes;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactItemModelTest {
    @BeforeAll
    static void initializeForgeMappings() {
        ForgeReflectionEnvironment.initialize();
    }

    private static int retainedCompactArrayCount(final Object root) throws IllegalAccessException {
        return retainedCompactArrayCount(root, new ReferenceOpenHashSet<>());
    }

    private static int retainedCompactArrayCount(final Object root, final ReferenceOpenHashSet<Object> seen)
        throws IllegalAccessException {
        if (root == null || !seen.add(root)) return 0;
        if (root.getClass().isArray()) return 1;
        if (!(root instanceof CompactItemModel) && root.getClass().getEnclosingClass() != CompactItemModel.class)
            return 0;
        int arrays = 0;
        for (final var field : root.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true);
            arrays += retainedCompactArrayCount(field.get(root), seen);
        }
        return arrays;
    }

    private static Object invokeMixin(final String type, final String name, final Object... args) throws Exception {
        for (final var method : Class.forName(type).getDeclaredMethods())
            if (method.getName().equals(name)) {
                method.setAccessible(true);
                return method.invoke(null, args);
            }
        throw new NoSuchMethodException(type + "." + name);
    }

    private static void compare(final BakedQuad original, final BakedQuad actual) {
        assertEquals(original.getFace(), actual.getFace());
        assertEquals(original.getTintIndex(), actual.getTintIndex());
        assertSame(original.getSprite(), actual.getSprite());
        assertEquals(original.shouldApplyDiffuseLighting(), actual.shouldApplyDiffuseLighting());
        assertArrayEquals(original.getVertexData(), actual.getVertexData());
        assertArrayEquals(readFloats(original), readFloats(actual));
    }

    private static int[] readFloats(final BakedQuad quad) {
        final int[] values = new int[4 * DefaultVertexFormats.ITEM.getElementCount() * 6];
        quad.pipe(new IVertexConsumer() {
            private int next;

            @Override
            public VertexFormat getVertexFormat() {
                return DefaultVertexFormats.ITEM;
            }

            @Override
            public void setQuadTint(final int tint) {
            }

            @Override
            public void setQuadOrientation(final EnumFacing face) {
            }

            @Override
            public void setApplyDiffuseLighting(final boolean diffuse) {
            }

            @Override
            public void setTexture(final TextureAtlasSprite sprite) {
            }

            @Override
            public void put(final int element, final float... data) {
                values[next++] = element;
                values[next++] = data.length;
                for (final float value : data) values[next++] = Float.floatToRawIntBits(value);
            }
        });
        return values;
    }

    @Test
    void orderedGeometryMatchesRealForgeAcrossAnimationAlphaAndRectangles() {
        final Random random = new Random(0x53ba7181L);
        for (final int[] dimensions : new int[][]{{1, 1}, {7, 13}, {13, 7}, {16, 16}, {17, 31}, {32, 32}, {256, 1}, {1, 256}}) {
            final int width = dimensions[0], height = dimensions[1];
            for (int test = 0; test < 6; test++) {
                final int[][] frames = new int[test % 3 + 1][width * height];
                for (final int[] frame : frames)
                    for (int i = 0; i < frame.length; i++) {
                        final int sample = random.nextInt(8);
                        final int alpha = test % 2 == 0 ? (sample < 4 ? 0 : 255) : new int[]{0, 25, 26, 127, 254, 255, 0, 255}[sample];
                        frame[i] = alpha << 24;
                    }
                final Sprite sprite = new Sprite(width, height, frames);
                final ItemContour contour = ItemContour.create(width, height, frames);
                assertNotNull(contour);
                final List<BakedQuad> original = ItemLayerModel.getQuadsForSprite(3, sprite, DefaultVertexFormats.ITEM, Optional.empty());
                assertEquals(original.size(), contour.size() + 2);
                final ItemQuadView cursor = new ItemQuadView();
                for (int i = 0; i < original.size(); i++) {
                    cursor.decode(contour, sprite, 3, i);
                    compare(original.get(i), cursor);
                    compare(original.get(i), cursor.materialize());
                }
            }
        }
    }

    @Test
    void listsOwnTheirCursorAndCompatibilityReadsPublishStableSharedGuiQuads() throws Exception {
        final int[][] pixels = {{-1, 0, -1, -1}};
        final Sprite sprite = new Sprite(2, 2, pixels);
        final ItemContour contour = ItemContour.create(2, 2, pixels);
        final CompactItemModel model = new CompactItemModel(new ItemContour[]{contour, contour}, new TextureAtlasSprite[]{sprite, sprite},
            sprite, ImmutableMap.of(), ItemOverrideList.NONE);
        assertFalse(model.novaeng$isGeometryExposed());
        assertEquals(3, retainedCompactArrayCount(model));
        final List<BakedQuad> first = model.novaeng$readQuads(null, null, 0);
        final var cursorField = first.getClass().getDeclaredField("cursor");
        cursorField.setAccessible(true);
        assertEquals((contour.size() + 2) * 2, first.size());
        assertNull(cursorField.get(first));
        final BakedQuad held = first.get(0);
        assertSame(held, cursorField.get(first));
        final int[] before = held.getVertexData().clone();
        final List<BakedQuad> second = model.novaeng$readQuads(null, null, 0);
        assertNotSame(held, second.get(1));
        assertArrayEquals(before, held.getVertexData());
        final IBakedModel gui = model.handlePerspective(TransformType.GUI).getLeft();
        assertEquals(2, ((CompactItemModel) gui).novaeng$readQuads(null, null, 0).size());
        final List<BakedQuad> stable = model.getQuads(null, null, 0);
        assertTrue(model.novaeng$isGeometryExposed());
        assertEquals(0, retainedCompactArrayCount(model), "Published models must release contours, sprites and layer ends");
        assertEquals(0, retainedCompactArrayCount(first), "Previously obtained lists must not retain compact arrays");
        assertEquals((contour.size() + 2) * 2, first.size());
        assertSame(stable, model.getQuads(null, null, 1));
        assertSame(stable.get(0), first.get(0));
        assertSame(stable.get(contour.size() + 1), gui.getQuads(null, null, 0).get(0));
        assertSame(model, gui.handlePerspective(TransformType.FIRST_PERSON_LEFT_HAND).getLeft());
        final BakedQuad south = gui.getQuads(null, null, 0).get(0);
        final int[] packed = south.getVertexData();
        packed[0] ^= 0x531b;
        assertEquals(packed[0], model.novaeng$readQuads(null, null, 0).get(contour.size() + 1).getVertexData()[0]);
        assertTrue(model.getQuads(null, EnumFacing.SOUTH, 0).isEmpty());
        assertThrows(UnsupportedOperationException.class, stable::clear);
    }

    @Test
    void publicMaterializationCanReleaseCompactStateDuringTrustedDecode() throws Exception {
        final CountDownLatch decoding = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        final AtomicReference<Thread> reader = new AtomicReference<>();
        final AtomicBoolean paused = new AtomicBoolean();
        final int[][] pixels = {{-1, 0, -1, -1}};
        final Sprite sprite = new Sprite(2, 2, pixels) {
            @Override
            public float getMinU() {
                if (Thread.currentThread() == reader.get() && paused.compareAndSet(false, true)) {
                    decoding.countDown();
                    try {
                        assertTrue(resume.await(10, TimeUnit.SECONDS), "Publication did not finish during trusted decode");
                    } catch (final InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(interrupted);
                    }
                }
                return super.getMinU();
            }
        };
        final ItemContour contour = ItemContour.create(2, 2, pixels);
        final CompactItemModel model = new CompactItemModel(new ItemContour[]{contour}, new TextureAtlasSprite[]{sprite},
            sprite, ImmutableMap.of(), ItemOverrideList.NONE);
        final List<BakedQuad> trusted = model.novaeng$readQuads(null, null, 0);
        final CompactItemModel gui = (CompactItemModel) model.handlePerspective(TransformType.GUI).getLeft();
        final List<BakedQuad> trustedGui = gui.novaeng$readQuads(null, null, 0);
        try (final var executor = Executors.newSingleThreadExecutor()) {
            final var pending = executor.submit(() -> {
                reader.set(Thread.currentThread());
                return trusted.get(contour.size()).getVertexData().clone();
            });
            try {
                assertTrue(decoding.await(10, TimeUnit.SECONDS), "Trusted reader did not reach decode");
                final List<BakedQuad> published = model.getQuads(null, null, 0);
                assertEquals(0, retainedCompactArrayCount(model));
                resume.countDown();
                assertArrayEquals(published.get(contour.size()).getVertexData(), pending.get(10, TimeUnit.SECONDS));
                assertSame(published.get(0), trusted.get(0));
                assertSame(published.get(contour.size() + 1), trustedGui.get(0));
                assertEquals(1, trustedGui.size());
            } finally {
                resume.countDown();
            }
        }
    }

    @Test
    void nonItemFormatsFallBackToStableForgeQuadsAndSkipItemFastPaths() throws Exception {
        final int[][] pixels = {{-1, 0, -1, -1}};
        final Sprite sprite = new Sprite(2, 2, pixels);
        final CompactItemModel model = new CompactItemModel(new ItemContour[]{ItemContour.create(2, 2, pixels)},
            new TextureAtlasSprite[]{sprite}, sprite, ImmutableMap.of(), ItemOverrideList.NONE, Attributes.DEFAULT_BAKED_FORMAT);
        assertFalse(model.novaeng$isItemFormat());
        final CallbackInfo callback = new CallbackInfo("renderLitItem", true);
        invokeMixin("github.kasuminova.novaeng.mixin.geometry.MixinGeometryLitItem", "novaeng$renderLayers", null, model,
            -1, null, callback);
        assertFalse(callback.isCancelled(), "Non-ITEM geometry must continue through the original renderer");
        assertFalse(MixinGeometryDisplayLists.isStableModel(model));
        assertFalse(model.novaeng$isGeometryExposed());
        final List<BakedQuad> quads = model.novaeng$readQuads(null, null, 0);
        assertTrue(model.novaeng$isGeometryExposed());
        assertSame(quads, model.getQuads(null, null, 0));
        assertSame(Attributes.DEFAULT_BAKED_FORMAT, quads.get(0).getFormat());
        final List<BakedQuad> expected = ItemLayerModel.getQuadsForSprite(0, sprite, Attributes.DEFAULT_BAKED_FORMAT, Optional.empty());
        for (int i = 0; i < quads.size(); i++) compare(expected.get(i), quads.get(i));
    }

    @Test
    void contourIdentityIncludesDimensionsAndOrderedAnimationUnion() {
        final int[][] pixels = {{-1, 0, 0, -1}};
        final ItemContour square = ItemContour.create(2, 2, pixels);
        assertEquals(square, ItemContour.create(2, 2, new int[][]{pixels[0].clone()}));
        assertEquals(square.hashCode(), ItemContour.create(2, 2, pixels).hashCode());
        assertNotEquals(square, ItemContour.create(4, 1, pixels));
        assertNull(ItemContour.create(257, 1, new int[][]{new int[257]}));
        assertEquals(0, ItemContour.create(1, 1, new int[0][]).size());
        assertNull(ItemContour.create(1, 1, new int[][]{null}));
    }

    @Test
    void publicInputsRemainDefensiveWhilePrivateBakeCanTransferItsArraysAndManyLayerLookupKeepsOrder() {
        final int[][] pixels = {{-1}};
        final Sprite sprite = new Sprite(1, 1, pixels);
        final ItemContour contour = ItemContour.create(1, 1, pixels);
        final ItemContour[] contours = new ItemContour[128];
        final TextureAtlasSprite[] sprites = new TextureAtlasSprite[contours.length];
        java.util.Arrays.fill(contours, contour);
        java.util.Arrays.fill(sprites, sprite);
        final CompactItemModel defensive = new CompactItemModel(contours, sprites, sprite, ImmutableMap.of(), ItemOverrideList.NONE);
        contours[0] = null;
        sprites[0] = null;
        assertSame(sprite, defensive.novaeng$readQuads(null, null, 0).getFirst().getSprite());
        contours[0] = contour;
        sprites[0] = sprite;
        final CompactItemModel owned = CompactItemModel.fromOwnedLayers(contours, sprites, sprite, ImmutableMap.of(),
            ItemOverrideList.NONE, DefaultVertexFormats.ITEM);
        final List<BakedQuad> actual = owned.novaeng$readQuads(null, null, 0);
        final int perLayer = contour.size() + 2;
        for (int index = actual.size() - 1; index >= 0; index--)
            assertEquals(index / perLayer, actual.get(index).getTintIndex());
    }

    @Test
    void pipeRetainsIndependentVertexArraysWithoutExposingCursorScratch() {
        final int[][] pixels = {{-1}};
        final Sprite sprite = new Sprite(1, 1, pixels);
        final ItemContour contour = ItemContour.create(1, 1, pixels);
        final ItemQuadView cursor = new ItemQuadView();
        cursor.decode(contour, sprite, 2, contour.size());
        final int[] packed = cursor.getVertexData().clone();
        final List<float[]> retained = new ObjectArrayList<>();
        cursor.pipe(new IVertexConsumer() {
            public VertexFormat getVertexFormat() {return DefaultVertexFormats.ITEM;}
            public void setQuadTint(int tint) { }
            public void setQuadOrientation(EnumFacing face) { }
            public void setApplyDiffuseLighting(boolean diffuse) { }
            public void setTexture(TextureAtlasSprite texture) { }
            public void put(int element, float... values) {retained.add(values);}
        });
        final int stride = DefaultVertexFormats.ITEM.getElementCount();
        assertNotSame(retained.get(0), retained.get(stride));
        final int nextVertex = Float.floatToRawIntBits(retained.get(stride)[0]);
        retained.get(0)[0] = 999F;
        assertEquals(nextVertex, Float.floatToRawIntBits(retained.get(stride)[0]));
        assertArrayEquals(packed, cursor.getVertexData());
        cursor.decode(contour, sprite, 3, contour.size() + 1);
        cursor.getVertexData();
        assertEquals(999F, retained.get(0)[0]);
    }

    @Test
    void productionBakeUsesOrdinarySpriteAndFallbackReplaysStateAndGetterOnlyOnce() throws Exception {
        final var constructor = TextureAtlasSprite.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        final TextureAtlasSprite ordinary = constructor.newInstance("production-item-test");
        ordinary.setIconWidth(2);
        ordinary.setIconHeight(2);
        ordinary.initSprite(1024, 1024, 73, 97, false);
        ordinary.setFramesTextureData(Collections.singletonList(new int[][]{{-1, 0, -1, -1}}));
        final TextureAtlasSprite inherited = new Sprite(2, 2, new int[][]{{-1, 0, -1, -1}});
        final TextureAtlasSprite custom = new CustomSprite();
        for (final TextureAtlasSprite sprite : List.of(ordinary, inherited, custom)) {
            for (final VertexFormat format : List.of(DefaultVertexFormats.ITEM, new VertexFormat(DefaultVertexFormats.ITEM),
                Attributes.DEFAULT_BAKED_FORMAT, new VertexFormat(Attributes.DEFAULT_BAKED_FORMAT))) {
                for (final boolean releasedFrames : List.of(false, true)) {
                    for (final boolean transformed : List.of(false, true)) {
                        sprite.setFramesTextureData(releasedFrames ? Collections.emptyList()
                            : Collections.singletonList(new int[][]{{-1, 0, -1, -1}}));
                        final Optional<TRSRTransformation> rootTransform = transformed
                            ? Optional.of(new TRSRTransformation(new javax.vecmath.Vector3f(.125F, 0, 0), null, null, null)) : Optional.empty();
                        final List<String> calls = new ObjectArrayList<>();
                        final IModelState state = part -> {
                            calls.add(part.isPresent() ? part.get().toString() : "root");
                            return part.isEmpty() ? rootTransform : Optional.empty();
                        };
                        final Function<ResourceLocation, TextureAtlasSprite> getter = location -> {
                            calls.add(location.toString());
                            return sprite;
                        };
                        final ImmutableList<ResourceLocation> textures = ImmutableList.of(new ResourceLocation("test:layer"));
                        final BakeHook hook = new BakeHook();
                        final var textureField = MixinItemLayerModel.class.getDeclaredField("textures");
                        textureField.setAccessible(true);
                        textureField.set(hook, textures);
                        final var overridesField = MixinItemLayerModel.class.getDeclaredField("overrides");
                        overridesField.setAccessible(true);
                        overridesField.set(hook, ItemOverrideList.NONE);
                    final IBakedModel result = hook.bake(state, format, getter);
                        assertEquals(sprite != custom && !transformed, result instanceof CompactItemModel);
                        final List<String> observed = Collections.unmodifiableList(new ObjectArrayList<>(calls));
                        calls.clear();
                        final IBakedModel baseline = new ItemLayerModel(textures).bake(state, format, getter);
                        assertEquals(calls, observed);
                        final List<BakedQuad> expected = baseline.getQuads(null, null, 0);
                        final List<BakedQuad> actual = result instanceof CompactItemModel compact ? compact.novaeng$readQuads(null, null, 0) : result.getQuads(null, null, 0);
                        assertEquals(expected.size(), actual.size());
                        if (releasedFrames) assertEquals(2, actual.size());
                        for (int i = 0; i < expected.size(); i++) {
                            assertSame(format, actual.get(i).getFormat());
                            compare(expected.get(i), actual.get(i));
                        }
                        final List<BakedQuad> stable = result.getQuads(null, null, 0);
                        for (int i = 0; i < expected.size(); i++) {
                            assertSame(format, stable.get(i).getFormat());
                            compare(expected.get(i), stable.get(i));
                        }
                    }
                }
            }
        }
    }

    @Test
    void unknownFormatsUseTheVanillaBakeAndKeepFailureTimingAndCallbackOrder() throws Exception {
        final Sprite sprite = new Sprite(2, 2, new int[][]{{-1, 0, -1, -1}});
        for (final ImmutableList<ResourceLocation> textures : List.of(ImmutableList.<ResourceLocation>of(),
            ImmutableList.of(new ResourceLocation("test:layer")))) {
            for (final VertexFormat format : List.of(DefaultVertexFormats.POSITION, new VertexFormat())) {
                final List<String> calls = new ObjectArrayList<>();
                final IModelState state = part -> { calls.add(part.isEmpty() ? "root" : part.get().toString()); return Optional.empty(); };
                final Function<ResourceLocation, TextureAtlasSprite> getter = location -> { calls.add(location.toString()); return sprite; };
                final BakeHook hook = new BakeHook();
                final var textureField = MixinItemLayerModel.class.getDeclaredField("textures");
                textureField.setAccessible(true);
                textureField.set(hook, textures);
                final var overridesField = MixinItemLayerModel.class.getDeclaredField("overrides");
                overridesField.setAccessible(true);
                overridesField.set(hook, ItemOverrideList.NONE);
                IBakedModel actual = null;
                RuntimeException failure = null;
                try { actual = hook.bake(state, format, getter); }
                catch (final RuntimeException exception) { failure = exception; }
                final List<String> observed = Collections.unmodifiableList(new ObjectArrayList<>(calls));
                calls.clear();
                if (failure != null) {
                    final RuntimeException expected = assertThrows(failure.getClass(), () -> new ItemLayerModel(textures).bake(state, format, getter));
                    assertEquals(expected.getMessage(), failure.getMessage());
                } else {
                    final IBakedModel expected = new ItemLayerModel(textures).bake(state, format, getter);
                    assertEquals(expected.getQuads(null, null, 0).size(), actual.getQuads(null, null, 0).size());
                    assertSame(expected.getParticleTexture(), actual.getParticleTexture());
                }
                assertEquals(calls, observed);
            }
        }
    }

    private static final class BakeHook extends MixinItemLayerModel {
    }

    private static class Sprite extends TextureAtlasSprite {
        Sprite(final int width, final int height, final int[][] frames) {
            super("compact-item-test");
            setIconWidth(width);
            setIconHeight(height);
            initSprite(8192, 4096, 173, 251, false);
            final List<int[][]> data = new ObjectArrayList<>();
            for (final int[] frame : frames) data.add(new int[][]{frame});
            setFramesTextureData(data);
        }
    }

    private static final class CustomSprite extends Sprite {
        CustomSprite() {
            super(2, 2, new int[][]{{-1, 0, -1, -1}});
        }

        @Override
        public float getMinU() {
            return super.getMinU();
        }
    }
}
