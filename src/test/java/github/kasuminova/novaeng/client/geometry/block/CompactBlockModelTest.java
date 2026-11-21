package github.kasuminova.novaeng.client.geometry.block;

import github.kasuminova.novaeng.client.geometry.CompactModel;
import github.kasuminova.novaeng.client.geometry.GeometryGeneration;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactBlockModelTest {
    private static CompactBlockModel compactProduct(final SimpleBakedModel source) {
        return CompactBlockModel.fromVanillaBake(source);
    }

    private static BakedQuad quad(final Sprite sprite, final int tint, final boolean diffuse) {
        return new BakedQuad(BlockGeometryRecordTest.rectangle(2, new int[]{0, 1, 3, 2}, 0.5F, 0F, 1F, 0F, 1F),
            tint, EnumFacing.SOUTH, sprite, diffuse, DefaultVertexFormats.ITEM);
    }

    private static SimpleBakedModel model(final Sprite sprite, final BakedQuad... quads) {
        final EnumMap<EnumFacing, List<BakedQuad>> sides = new EnumMap<>(EnumFacing.class);
        for (final EnumFacing face : EnumFacing.values()) sides.put(face, new ArrayList<>());
        return new SimpleBakedModel(new ArrayList<>(List.of(quads)), sides, true, false,
            sprite, ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE);
    }

    @Test
    void propertiesOrderAndStableWritablePublicationArePreserved() {
        final Sprite sprite = new Sprite();
        final BakedQuad a = quad(sprite, 2, true);
        final SimpleBakedModel original = model(sprite, a, quad(sprite, 7, false), quad(sprite, 2, true));
        final CompactBlockModel compact = compactProduct(original);
        assertEquals(original.isAmbientOcclusion(), compact.isAmbientOcclusion());
        assertEquals(original.isGui3d(), compact.isGui3d());
        assertSame(sprite, compact.getParticleTexture());
        assertSame(original.getOverrides(), compact.getOverrides());
        assertSame(original.getItemCameraTransforms(), compact.getItemCameraTransforms());
        final List<BakedQuad> trusted = compact.novaeng$readQuads(null, null, 0);
        assertFalse(compact.novaeng$isGeometryExposed());
        assertEquals(List.of(2, 7, 2), trusted.stream().map(BakedQuad::getTintIndex).toList());
        assertArrayEquals(a.vertexData, trusted.get(0).vertexData);
        assertFalse(trusted.get(1).shouldApplyDiffuseLighting());
        final List<BakedQuad> published = compact.getQuads(null, null, 0);
        assertTrue(compact.novaeng$isGeometryExposed());
        assertSame(published, compact.getQuads(null, null, 42));
        assertNotSame(published.get(0), published.get(2));
        assertNotSame(published.get(0).vertexData, published.get(2).vertexData);
        published.get(0).vertexData[0] = 0x7FA12345;
        assertEquals(0x7FA12345, trusted.get(0).vertexData[0]);
        assertEquals(a.vertexData[0], compact.novaeng$readQuads(null, null, 0).get(2).vertexData[0]);
        published.remove(1);
        assertEquals(2, trusted.size());
        assertSame(published, compact.novaeng$readQuads(null, null, 0));
    }

    @Test
    void independentVanillaModelsNeverSharePublishedMutableObjects() {
        final Sprite sprite = new Sprite();
        final IBakedModel first, second;
        try (final GeometryGeneration ignored = GeometryGeneration.begin()) {
            first = compactProduct(model(sprite, quad(sprite, 1, true)));
            second = compactProduct(model(sprite, quad(sprite, 1, true)));
        }
        final List<BakedQuad> a = first.getQuads(null, null, 0), b = second.getQuads(null, null, 0);
        assertNotSame(a, b);
        assertNotSame(a.getFirst(), b.getFirst());
        assertNotSame(a.getFirst().vertexData, b.getFirst().vertexData);
        final int original = b.getFirst().vertexData[0];
        a.getFirst().vertexData[0] = 987;
        assertEquals(original, b.getFirst().vertexData[0]);
        a.clear();
        assertEquals(1, b.size());
    }

    @Test
    void publishedCullListsRemainIndependentlyWritable() {
        final Sprite sprite = new Sprite();
        final CompactBlockModel compact = compactProduct(model(sprite, quad(sprite, 1, true)));
        final List<BakedQuad> general = compact.getQuads(null, null, 0);
        final List<BakedQuad> down = compact.getQuads(null, EnumFacing.DOWN, 0);
        assertNotSame(general, down);
        down.add(quad(sprite, 2, true));
        assertEquals(1, general.size());
        assertSame(down, compact.getQuads(null, EnumFacing.DOWN, 42));
        down.clear();
        assertEquals(1, general.size());
    }

    @Test
    void actualCleanroomVanillaBuilderAndFaceBakeryRoundTripEveryFacing() {
        final Sprite sprite = new Sprite();
        final ModelBlock source = new ModelBlock(null, List.of(), Map.of(), true, true,
            ItemCameraTransforms.DEFAULT, List.of());
        final SimpleBakedModel.Builder builder = new SimpleBakedModel.Builder(source, ItemOverrideList.NONE).setTexture(sprite);
        final FaceBakery bakery = new FaceBakery();
        final BakedQuad[] original = new BakedQuad[6];
        for (final EnumFacing facing : EnumFacing.values()) {
            final BakedQuad quad = bakery.makeBakedQuad(new Vector3f(0F, 1F, 2F), new Vector3f(16F, 15F, 14F),
                new BlockPartFace(null, 4, "test", new BlockFaceUV(new float[]{0F, 0F, 16F, 16F}, 0)), sprite,
                facing, ModelRotation.X0_Y0, null, false, true);
            assertEquals(BakedQuad.class, quad.getClass());
            original[facing.getIndex()] = quad;
            builder.addFaceQuad(facing, quad);
        }
        final SimpleBakedModel product = (SimpleBakedModel) builder.makeBakedModel();
        final CompactBlockModel compact = compactProduct(product);
        for (final EnumFacing facing : EnumFacing.values()) {
            final BakedQuad decoded = compact.novaeng$readQuads(null, facing, 0).getFirst();
            assertArrayEquals(original[facing.getIndex()].vertexData, decoded.vertexData);
            assertEquals(facing, decoded.getFace());
        }
        assertTrue(compact.getQuads(null, null, 0).isEmpty());
    }

    @Test
    void lazyUnpackedLightmapQuadsArePackedBeforeCapture() {
        final Sprite sprite = new Sprite();
        // AE2's online controller light faces are built as lazy UnpackedBakedQuads with UV1.
        final VertexFormat format = DefaultVertexFormats.BLOCK;
        final UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
        builder.setTexture(sprite);
        builder.setQuadOrientation(EnumFacing.SOUTH);
        builder.setQuadTint(7);
        builder.setApplyDiffuseLighting(false);
        for (int vertex = 0; vertex < 4; vertex++) {
            final int corner = new int[]{0, 1, 3, 2}[vertex];
            for (int element = 0; element < format.getElementCount(); element++) {
                final var value = format.getElement(element);
                switch (value.getUsage()) {
                    case POSITION -> builder.put(element, corner & 1, (corner >>> 1) & 1, 0.5F, 1F);
                    case COLOR -> builder.put(element, 0.25F, 0.5F, 1F, 1F);
                    case UV -> {
                        if (value.getIndex() == 0) builder.put(element, 0.125F + (corner & 1) * 0.5F,
                            0.25F + ((corner >>> 1) & 1) * 0.5F);
                        else builder.put(element, 0.75F, 1F);
                    }
                    default -> builder.put(element);
                }
            }
        }
        final UnpackedBakedQuad original = builder.build();
        assertEquals(UnpackedBakedQuad.class, original.getClass());
        assertTrue(Arrays.stream(original.vertexData).allMatch(word -> word == 0),
            "Regression must start with an uninitialized packed array");
        final EnumMap<EnumFacing, List<BakedQuad>> sides = new EnumMap<>(EnumFacing.class);
        for (final EnumFacing face : EnumFacing.values()) sides.put(face,
            face == EnumFacing.SOUTH ? List.of(original) : List.of());
        final CompactBlockModel compact = compactProduct(new SimpleBakedModel(List.of(original), sides, true,
            false, sprite, ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE));
        final int[] expected = original.getVertexData();
        assertTrue(Arrays.stream(expected).anyMatch(word -> word != 0));
        for (final EnumFacing side : new EnumFacing[]{null, EnumFacing.SOUTH}) {
            final BakedQuad decoded = compact.novaeng$readQuads(null, side, 0).getFirst();
            assertArrayEquals(expected, decoded.getVertexData());
            assertSame(format, decoded.getFormat());
            assertSame(sprite, decoded.getSprite());
            assertEquals(EnumFacing.SOUTH, decoded.getFace());
            assertEquals(7, decoded.getTintIndex());
            assertFalse(decoded.shouldApplyDiffuseLighting());
            for (int vertex = 0; vertex < 4; vertex++) {
                final int base = vertex * format.getIntegerSize();
                assertEquals(255, decoded.getVertexData()[base + format.getColorOffset() / 4] >>> 24);
                assertNotEquals(0, decoded.getVertexData()[base + format.getUvOffsetById(1) / 4]);
            }
            assertArrayEquals(expected, compact.getQuads(null, side, 0).getFirst().getVertexData());
        }
    }

    @Test
    void temporaryReadListDecodesEachFaceOnceWithoutAModelVertexCache() {
        final Sprite sprite = new Sprite();
        final CompactModel compact = compactProduct(model(sprite, quad(sprite, 1, true), quad(sprite, 2, true)));
        final List<BakedQuad> read = compact.novaeng$readQuads(null, null, 0);
        assertSame(read.get(0), read.get(0));
        assertSame(read.get(1), read.get(1));
        assertNotSame(read.get(0), compact.novaeng$readQuads(null, null, 0).get(0));
        final CompactModel single = compactProduct(model(sprite, quad(sprite, 1, true)));
        final List<BakedQuad> singleRead = single.novaeng$readQuads(null, null, 0);
        assertSame(singleRead.get(0), singleRead.get(0));
        assertFalse(compact.novaeng$isGeometryExposed());
    }

    @Test
    void bakeGenerationSharesPrivateSequencesIndependentlyOfSprite() throws Exception {
        final Sprite sprite = new Sprite();
        try (final GeometryGeneration ignored = GeometryGeneration.begin()) {
            final CompactBlockModel first = compactProduct(model(sprite, quad(sprite, 1, true)));
            final CompactBlockModel second = compactProduct(model(sprite, quad(sprite, 1, true)));
            final Sprite otherSprite = new Sprite();
            final CompactBlockModel other = compactProduct(model(otherSprite, quad(otherSprite, 1, true)));
            final var storage = CompactBlockModel.class.getDeclaredField("storage");
            storage.setAccessible(true);
            final Object a = ((Object[]) storage.get(first))[6], b = ((Object[]) storage.get(second))[6], c = ((Object[]) storage.get(other))[6];
            assertSame(a, b);
            assertNotSame(a, c);
            final var ids = a.getClass().getDeclaredField("ids");
            ids.setAccessible(true);
            final long idsA = ids.getLong(a), idsC = ids.getLong(c);
            assertEquals(idsA & (BlockGeometryPages.PAGE_SIZE - 1), idsC & (BlockGeometryPages.PAGE_SIZE - 1));
            assertNotEquals(idsA >>> 12 & (BlockGeometryPages.PAGE_SIZE - 1), idsC >>> 12 & (BlockGeometryPages.PAGE_SIZE - 1));
        }
    }

    @Test
    void exposingDropsCompactStorageWhileEarlierTrustedViewsRemainLive() throws Exception {
        final Sprite sprite = new Sprite();
        final CompactBlockModel compact;
        try (final GeometryGeneration ignored = GeometryGeneration.begin()) {
            compact = compactProduct(model(sprite, quad(sprite, 2, true)));
        }
        for (final var field : CompactBlockModel.class.getDeclaredFields())
            assertFalse(IBakedModel.class.isAssignableFrom(field.getType()), "Compact models must not retain the producer");
        final List<BakedQuad> inFlight = compact.novaeng$readQuads(null, null, 0);
        final List<BakedQuad> published = compact.getQuads(null, null, 0);
        final var storage = CompactBlockModel.class.getDeclaredField("storage");
        storage.setAccessible(true);
        assertNull(storage.get(compact));
        published.getFirst().vertexData[0] = 123;
        assertEquals(123, inFlight.getFirst().vertexData[0]);
    }

    @Test
    void largeFaceListsKeepEveryQuadInOrderAfterSeal() {
        final Sprite sprite = new Sprite();
        final BakedQuad[] quads = new BakedQuad[135];
        for (int index = 0; index < quads.length; index++) quads[index] = quad(sprite, index, true);
        final CompactBlockModel compact;
        try (final GeometryGeneration ignored = GeometryGeneration.begin()) {
            compact = compactProduct(model(sprite, quads));
        }
        final List<BakedQuad> published = compact.getQuads(null, null, 0);
        assertEquals(quads.length, published.size());
        for (int index = 0; index < quads.length; index++) {
            assertEquals(index, published.get(index).getTintIndex());
            assertArrayEquals(quads[index].vertexData, published.get(index).vertexData);
            if (index != 0) assertNotSame(published.get(index - 1).vertexData, published.get(index).vertexData);
        }
    }

    private static final class Sprite extends TextureAtlasSprite {
        private Sprite() {
            super("geometry-test");
        }
    }
}
