package github.kasuminova.novaeng.client.geometry.block;

import github.kasuminova.novaeng.client.geometry.CompactModel;
import github.kasuminova.novaeng.client.geometry.GeometryGeneration;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import org.lwjgl.util.vector.Vector3f;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standalone synthetic object-graph benchmark. Run as a startup agent; never starts Minecraft.
 */
public final class BlockGeometryMemoryBenchmark {
    private static final int MODEL_COUNT = 4096;
    private static Instrumentation instrumentation;

    public static void premain(final String arguments, final Instrumentation supplied) {
        instrumentation = supplied;
    }

    static void main(final String[] arguments) throws Exception {
        if (arguments.length > 1 && arguments[1].equals("--stress")) {
            stress(arguments[0]);
            return;
        }
        if (instrumentation == null) throw new IllegalStateException("Run with the benchmark startup agent");
        final StringBuilder report = new StringBuilder("Synthetic FaceBakery object graph; 4096 models, 64 shapes, 8 sprites, six cull directions.\n")
            .append("Includes reachable model/list/map/page/quad/array objects; excludes shared sprite/format/camera/override/enum assets.\n")
            .append("Not a client heap or pack measurement; no renderer mixins applied.\n");
        final IBakedModel[] baseline = pooledBaseline();
        append(report, "old_quad_and_list_pools", baseline);
        final IBakedModel[] compact = new IBakedModel[MODEL_COUNT];
        try (final GeometryGeneration generation = GeometryGeneration.begin()) {
            final Sprite[] sprites = sprites();
            for (int index = 0; index < MODEL_COUNT; index++) compact[index] = compactSource(index, sprites);
            append(report, "compact_bake_snapshot_with_bounded_indices", new Object[]{compact, generation});
        }
        append(report, "compact_sealed", compact);
        for (int index = 0; index < MODEL_COUNT / 4; index++) compact[index].getQuads(null, null, 0);
        append(report, "compact_25_percent_exposed", compact);
        for (final IBakedModel model : compact) model.getQuads(null, null, 0);
        append(report, "compact_all_exposed", compact);
        report.append("JVM: ").append(System.getProperty("java.vm.name")).append(' ').append(System.getProperty("java.version")).append('\n');
        Files.writeString(Path.of(arguments[0]), report);
        System.out.print(report);
    }

    /**
     * Exercise reload-time retention in a heap too small to retain the original products as well.
     */
    private static void stress(final String output) throws Exception {
        final long started = System.nanoTime();
        final int count = 200_000;
        final IBakedModel[] models = new IBakedModel[count];
        final Sprite[] sprites = sprites();
        try (final GeometryGeneration ignored = GeometryGeneration.begin()) {
            for (int index = 0; index < count; index++) models[index] = compactSource(index, sprites);
            // Keep the generation open through a full collection, exposing peak retained ownership.
            System.gc();
            final long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            final String report = "Synthetic owned block stress: " + count + " models / " + count * 6 + " quads, generation still open; used after GC "
                + used + " bytes; elapsed " + (System.nanoTime() - started) / 1_000_000 + " ms; max heap " + Runtime.getRuntime().maxMemory() + " bytes\n";
            Files.writeString(Path.of(output), report);
            System.out.print(report);
        }
        // Prove the model set was retained until the last generation-open measurement.
        if (((CompactModel) models[count - 1]).novaeng$readQuads(null, EnumFacing.UP, 0).size() != 1)
            throw new AssertionError("Lost final model");
    }

    private static IBakedModel compactSource(final int index, final Sprite[] sprites) {
        return CompactBlockModel.fromVanillaBake((SimpleBakedModel) source(index, sprites));
    }

    private static IBakedModel[] pooledBaseline() {
        final IBakedModel[] models = new IBakedModel[MODEL_COUNT];
        final Map<QuadKey, BakedQuad> quads = new HashMap<>();
        final Map<List<BakedQuad>, List<BakedQuad>> lists = new HashMap<>();
        final Sprite[] sprites = sprites();
        for (int index = 0; index < models.length; index++) {
            final IBakedModel source = source(index, sprites);
            final EnumMap<EnumFacing, List<BakedQuad>> sides = new EnumMap<>(EnumFacing.class);
            for (final EnumFacing face : EnumFacing.values()) {
                final List<BakedQuad> contents = new ArrayList<>();
                for (final BakedQuad quad : source.getQuads(null, face, 0))
                    contents.add(quads.computeIfAbsent(new QuadKey(quad), ignored -> quad));
                sides.put(face, lists.computeIfAbsent(contents, ignored -> contents));
            }
            final List<BakedQuad> empty = lists.computeIfAbsent(new ArrayList<>(), ignored -> new ArrayList<>());
            models[index] = new SimpleBakedModel(empty, sides, true, true, source.getParticleTexture(), ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE);
        }
        return models;
    }

    private static IBakedModel source(final int index, final Sprite[] sprites) {
        final Sprite sprite = sprites[index % sprites.length];
        final int shape = index / sprites.length % 64;
        final FaceBakery bakery = new FaceBakery();
        final EnumMap<EnumFacing, List<BakedQuad>> faces = new EnumMap<>(EnumFacing.class);
        for (final EnumFacing face : EnumFacing.values()) {
            final BakedQuad quad = bakery.makeBakedQuad(new Vector3f(shape % 8, shape / 8, 0F), new Vector3f(16F, 16F, 16F),
                new BlockPartFace(face, -1, "test", new BlockFaceUV(new float[]{0F, 0F, 16F, 16F}, 0)), sprite,
                face, ModelRotation.X0_Y0, null, false, true);
            faces.put(face, new ArrayList<>(List.of(quad)));
        }
        return new SimpleBakedModel(new ArrayList<>(), faces, true, true, sprite, ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE);
    }

    private static Sprite[] sprites() {
        final Sprite[] sprites = new Sprite[8];
        for (int index = 0; index < sprites.length; index++) sprites[index] = new Sprite(index);
        return sprites;
    }

    private static void append(final StringBuilder report, final String label, final Object roots) throws Exception {
        final ReferenceOpenHashSet<Object> visited = new ReferenceOpenHashSet<>();
        final ObjectArrayFIFOQueue<Object> pending = new ObjectArrayFIFOQueue<>();
        pending.enqueue(roots);
        long bytes = 0;
        while (!pending.isEmpty()) {
            final Object value = pending.dequeue();
            if (excluded(value) || !visited.add(value)) continue;
            bytes += instrumentation.getObjectSize(value);
            final Class<?> type = value.getClass();
            if (type.isArray()) {
                if (!type.componentType().isPrimitive()) for (int index = 0; index < Array.getLength(value); index++)
                    enqueue(pending, Array.get(value, index));
                continue;
            }
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (final var field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive() || field.getName().equals("keyUniverse"))
                        continue;
                    field.setAccessible(true);
                    enqueue(pending, field.get(value));
                }
            }
        }
        report.append(label).append(": ").append(bytes).append(" bytes, ").append(visited.size()).append(" objects\n");
    }

    private static void enqueue(final ObjectArrayFIFOQueue<Object> pending, final Object value) {
        if (value != null) pending.enqueue(value);
    }

    private static boolean excluded(final Object value) {
        return value instanceof TextureAtlasSprite || value instanceof VertexFormat
            || value instanceof ItemCameraTransforms || value instanceof ItemOverrideList || value instanceof Enum<?> || value instanceof Class<?> || value instanceof String;
    }

    private static final class Sprite extends TextureAtlasSprite {
        private Sprite(final int index) {
            super("benchmark-" + index);
            width = height = 16;
            initSprite(128, 16, index * 16, 0, false);
        }
    }

    private record QuadKey(BakedQuad quad) {

        @Override
        public boolean equals(final Object other) {
            return other instanceof QuadKey(
                BakedQuad quad1
            ) && quad.getSprite() == quad1.getSprite() && quad.getFace() == quad1.getFace()
                && quad.getTintIndex() == quad1.getTintIndex() && quad.shouldApplyDiffuseLighting() == quad1.shouldApplyDiffuseLighting()
                && quad.getFormat() == quad1.getFormat() && Arrays.equals(quad.vertexData, quad1.vertexData);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(quad.vertexData) * 31 + System.identityHashCode(quad.getSprite()) * 7 + quad.getFace().ordinal();
        }
    }
}
