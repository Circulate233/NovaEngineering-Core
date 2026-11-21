package github.kasuminova.novaeng.client.geometry.vertex;

import com.google.common.collect.ImmutableMap;
import github.kasuminova.novaeng.client.geometry.CompactModel;
import github.kasuminova.novaeng.client.geometry.TrustedGeometryList;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.PropertyFloat;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the real fluid producer, original property helpers and original bounded cache.
 */
class ForgeFluidCompatibilityTest {
    private static final String BASE = "net/minecraftforge/client/model/ModelFluid$BakedFluid";
    private static final String CACHE = "net/minecraftforge/client/model/ModelFluid$CachingBakedFluid";
    private static final String PREFIX = "github/kasuminova/novaeng/client/geometry/vertex/";
    private static final String MIXIN = "github/kasuminova/novaeng/mixin/geometry/vertex/";

    private static ClassNode read(final String source, final Map<String, String> names) throws Exception {
        final ClassNode node = new ClassNode();
        try (final var input = ForgeFluidCompatibilityTest.class.getClassLoader().getResourceAsStream(source + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(new ClassRemapper(node, new SimpleRemapper(names)), 0);
        }
        node.access = (node.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
        node.innerClasses.clear();
        node.nestHostClass = null;
        node.nestMembers = null;
        return node;
    }

    private static ClassLoader transformedFluid() throws Exception {
        final Class<?> builder = ForgeBuilderCompatibilityTest.transformedBuilder();
        final Map<String, String> names = new HashMap<>();
        names.put(BASE, PREFIX + "FluidBaseProbe");
        names.put(CACHE, PREFIX + "FluidCacheProbe");
        names.put(CACHE + "$1", PREFIX + "FluidCacheLoaderProbe");
        names.put(BASE + "$VertexParameter", PREFIX + "FluidVertexParameterProbe");
        names.put("net/minecraftforge/client/model/ModelFluid$1", PREFIX + "FluidEnumSwitchProbe");
        // Isolate Blocks' unrelated game-bootstrap initializer; retain the original state helpers.
        names.put("net/minecraftforge/fluids/BlockFluidBase", FluidProperties.class.getName().replace('.', '/'));
        names.put("net/minecraftforge/fluids/BlockFluidBase$UnlistedPropertyBool", BooleanProperty.class.getName().replace('.', '/'));
        names.put("net/minecraftforge/client/model/pipeline/UnpackedBakedQuad$Builder", builder.getName().replace('.', '/'));
        names.put(MIXIN + "MixinModelFluidOwnedGeometry", names.get(BASE));
        names.put(MIXIN + "MixinCachingBakedFluidRead", names.get(CACHE));
        final ClassNode base = read(BASE, names);
        final ClassNode mixin = read(MIXIN + "MixinModelFluidOwnedGeometry", names);
        base.interfaces.add(CompactModel.class.getName().replace('.', '/'));
        base.interfaces.add(OwnedFluidGeometry.class.getName().replace('.', '/'));
        base.fields.stream().filter(field -> field.name.equals("faceQuads"))
                   .forEach(field -> field.access &= ~Opcodes.ACC_FINAL);
        mixin.fields.stream().filter(field -> field.name.startsWith("novaeng$")).forEach(base.fields::add);
        mixin.methods.stream().filter(method -> method.name.startsWith("novaeng$")).forEach(base.methods::add);
        int redirects = 0;
        for (final var method : base.methods) {
            if (method.name.equals("buildQuad")) {
                for (final var instruction : method.instructions.toArray()) {
                    if (instruction instanceof MethodInsnNode invoke && invoke.owner.equals(names.get("net/minecraftforge/client/model/pipeline/UnpackedBakedQuad$Builder"))
                        && invoke.name.equals("build")) {
                        final InsnList injection = new InsnList();
                        injection.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        injection.add(new InsnNode(Opcodes.SWAP));
                        injection.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, names.get(BASE), "novaeng$capturePrivateBuilder",
                            "(L" + names.get("net/minecraftforge/client/model/pipeline/UnpackedBakedQuad$Builder")
                                + ";)Lnet/minecraftforge/client/model/pipeline/UnpackedBakedQuad;", false));
                        method.instructions.insertBefore(instruction, injection);
                        method.instructions.remove(instruction);
                        redirects++;
                    }
                }
            } else if (method.name.equals("getQuads")) {
                final InsnList injection = new InsnList();
                injection.add(new VarInsnNode(Opcodes.ALOAD, 0));
                injection.add(new VarInsnNode(Opcodes.ALOAD, 1));
                injection.add(new VarInsnNode(Opcodes.ALOAD, 2));
                injection.add(new VarInsnNode(Opcodes.LLOAD, 3));
                injection.add(new InsnNode(Opcodes.ACONST_NULL));
                injection.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, names.get(BASE), "novaeng$beforePublicRead",
                    "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;JLorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V", false));
                method.instructions.insert(injection);
            }
        }
        assertEquals(1, redirects, "Mapped fluid private producer changed");
        final ClassNode cached = read(CACHE, names);
        final ClassNode cachedMixin = read(MIXIN + "MixinCachingBakedFluidRead", names);
        cachedMixin.methods.stream().filter(method -> method.name.equals("novaeng$readQuads")).forEach(cached.methods::add);
        final Map<String, byte[]> classes = new HashMap<>();
        for (final ClassNode node : List.of(base, cached, read(CACHE + "$1", names),
            read(BASE + "$VertexParameter", names), read("net/minecraftforge/client/model/ModelFluid$1", names))) {
            final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
                @Override
                protected String getCommonSuperClass(final String left, final String right) {
                    String originalLeft = left;
                    String originalRight = right;
                    for (final var name : names.entrySet()) {
                        if (name.getValue().equals(left) && !name.getKey().startsWith(MIXIN))
                            originalLeft = name.getKey();
                        if (name.getValue().equals(right) && !name.getKey().startsWith(MIXIN))
                            originalRight = name.getKey();
                    }
                    final String common = super.getCommonSuperClass(originalLeft, originalRight);
                    return names.getOrDefault(common, common);
                }
            };
            node.accept(writer);
            classes.put(node.name.replace('/', '.'), writer.toByteArray());
        }
        return new ClassLoader(builder.getClassLoader()) {
            @Override
            protected Class<?> findClass(final String name) throws ClassNotFoundException {
                final byte[] bytes = classes.get(name);
                if (bytes == null) throw new ClassNotFoundException(name);
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
    }

    private static IBakedModel base(final Class<?> type, final TextureAtlasSprite sprite, final int[] corners,
                                    final int flow, final boolean gas) throws Exception {
        final var constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return (IBakedModel) constructor.newInstance(Optional.empty(), ImmutableMap.of(), DefaultVertexFormats.ITEM,
            0x80af3377, sprite, sprite, Optional.of(sprite), gas, true, corners, flow,
            new boolean[]{true, false, true, false});
    }

    private static IBakedModel caching(final Class<?> type, final TextureAtlasSprite sprite) throws Exception {
        final var constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return (IBakedModel) constructor.newInstance(Optional.empty(), ImmutableMap.of(), DefaultVertexFormats.ITEM,
            0xff336699, sprite, sprite, Optional.of(sprite), false, Optional.empty());
    }

    private static void compare(final List<BakedQuad> expected, final List<BakedQuad> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            final BakedQuad left = expected.get(i);
            final BakedQuad right = actual.get(i);
            assertArrayEquals(left.getVertexData(), right.getVertexData());
            assertArrayEquals(left.getVertexData(), right.vertexData);
            assertSame(left.getFormat(), right.getFormat());
            assertSame(left.getSprite(), right.getSprite());
            assertEquals(left.getFace(), right.getFace());
            assertEquals(left.getTintIndex(), right.getTintIndex());
            assertEquals(left.shouldApplyDiffuseLighting(), right.shouldApplyDiffuseLighting());
            final VertexFormat format = left.getFormat();
            final List<float[]> a = new ArrayList<>();
            final List<float[]> b = new ArrayList<>();
            left.pipe(LosslessFloatRecordTest.consumer(format, a));
            right.pipe(LosslessFloatRecordTest.consumer(format, b));
            assertEquals(a.size(), b.size());
            for (int element = 0; element < a.size(); element++)
                for (int c = 0; c < 4; c++) {
                    assertEquals(Float.floatToRawIntBits(a.get(element)[c]), Float.floatToRawIntBits(b.get(element)[c]));
                }
        }
    }

    @Test
    void denseSlopedFluidProducerIsBitExactAndPublicMaterializationIsStable() throws Exception {
        final ClassLoader loader = transformedFluid();
        final Class<?> optimizedType = loader.loadClass((PREFIX + "FluidBaseProbe").replace('/', '.'));
        final Class<?> originalType = Class.forName(BASE.replace('/', '.'));
        final var sprite = new LosslessFloatRecordTest.Sprite();
        for (final boolean gas : List.of(false, true)) {
            for (final int flow : new int[]{-1000, -67, 0, 179}) {
                final int[] corners = {111, 637, 299, 801};
                final IBakedModel original = base(originalType, sprite, corners, flow, gas);
                final IBakedModel optimized = base(optimizedType, sprite, corners, flow, gas);
                final CompactModel compact = (CompactModel) optimized;
                for (final EnumFacing side : EnumFacing.values()) {
                    final List<BakedQuad> expected = original.getQuads(null, side, 99);
                    final List<BakedQuad> trusted = compact.novaeng$readQuads(null, side, 99);
                    assertInstanceOf(TrustedGeometryList.class, trusted);
                    compare(expected, trusted);
                    assertFalse(compact.novaeng$isGeometryExposed());
                }
                final List<BakedQuad> first = optimized.getQuads(null, EnumFacing.UP, 99);
                assertTrue(compact.novaeng$isGeometryExposed());
                assertSame(first, optimized.getQuads(null, EnumFacing.UP, 100));
                assertSame(first, compact.novaeng$readQuads(null, EnumFacing.UP, 100));
                for (final BakedQuad quad : first)
                    assertEquals("net.minecraftforge.client.model.pipeline.UnpackedBakedQuad", quad.getClass().getName());
                first.getFirst().vertexData[0] = 1896;
                // First packed getter still overwrites direct pre-pack edits, exactly as Forge.
                first.getFirst().getVertexData();
                first.getFirst().vertexData[0] = 1896;
                assertEquals(1896, optimized.getQuads(null, EnumFacing.UP, 99).getFirst().getVertexData()[0]);
            }
        }
    }

    @Test
    void cachingFluidRetainsOriginalPropertyOrderQuantizationAndCacheSelection() throws Exception {
        final ClassLoader loader = transformedFluid();
        final var sprite = new LosslessFloatRecordTest.Sprite();
        final Class<?> cacheType = loader.loadClass((PREFIX + "FluidCacheProbe").replace('/', '.'));
        // getQuads is Forge's unchanged public cache selection body; novaeng$readQuads is the mixin.
        final IBakedModel original = caching(cacheType, sprite);
        final IBakedModel optimized = caching(cacheType, sprite);
        final List<String> calls = new ArrayList<>();
        final IExtendedBlockState state = (IExtendedBlockState) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{IExtendedBlockState.class}, (proxy, method, arguments) -> {
                if (!method.getName().equals("getValue")) throw new AssertionError(method);
                final Object property = arguments[0];
                for (int i = 0; i < 4; i++) {
                    if (property == FluidProperties.LEVEL_CORNERS[i]) {
                        calls.add("corner" + i);
                        return (i + 1) * 0.137f;
                    }
                    if (property == FluidProperties.SIDE_OVERLAYS[i]) {
                        calls.add("overlay" + i);
                        return i % 2 == 0;
                    }
                }
                if (property == FluidProperties.FLOW_DIRECTION) {
                    calls.add("flow");
                    return -0.731f;
                }
                throw new AssertionError(property);
            });
        for (final EnumFacing side : EnumFacing.values()) {
            calls.clear();
            final List<BakedQuad> expected = original.getQuads(state, side, 77);
            final List<String> expectedCalls = List.copyOf(calls);
            calls.clear();
            final List<BakedQuad> actual = ((CompactModel) optimized).novaeng$readQuads(state, side, 77);
            assertEquals(expectedCalls, calls);
            compare(expected, actual);
            final List<BakedQuad> publicQuads = optimized.getQuads(state, side, 78);
            compare(expected, publicQuads);
            assertSame(publicQuads, optimized.getQuads(state, side, 79));
        }
    }

    public static final class FluidProperties {
        public static final PropertyFloat[] LEVEL_CORNERS = {new PropertyFloat("corner0"), new PropertyFloat("corner1"),
            new PropertyFloat("corner2"), new PropertyFloat("corner3")};
        public static final PropertyFloat FLOW_DIRECTION = new PropertyFloat("flow");
        public static final BooleanProperty[] SIDE_OVERLAYS = {new BooleanProperty("overlay0"), new BooleanProperty("overlay1"),
            new BooleanProperty("overlay2"), new BooleanProperty("overlay3")};
    }

    public static final class BooleanProperty implements IUnlistedProperty<Boolean> {
        private final String name;

        public BooleanProperty(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isValid(final Boolean value) {
            return true;
        }

        @Override
        public Class<Boolean> getType() {
            return Boolean.class;
        }

        @Override
        public String valueToString(final Boolean value) {
            return value.toString();
        }
    }
}
