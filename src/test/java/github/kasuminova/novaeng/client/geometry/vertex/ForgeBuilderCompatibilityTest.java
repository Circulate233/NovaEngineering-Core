package github.kasuminova.novaeng.client.geometry.vertex;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraftforge.client.model.pipeline.IVertexConsumer;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
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
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Uses the actual mapped Forge class and production mixin bodies in an isolated test class.
 */
class ForgeBuilderCompatibilityTest {
    private static final String BUILDER = "net/minecraftforge/client/model/pipeline/UnpackedBakedQuad$Builder";
    private static final String MIXIN = "github/kasuminova/novaeng/mixin/geometry/vertex/MixinUnpackedBakedQuadBuilder";
    private static final String GENERATED = "github/kasuminova/novaeng/client/geometry/vertex/FlatBuilderProbe";

    private static ClassNode read(final String source, final String target) throws Exception {
        final ClassNode node = new ClassNode();
        try (final var input = ForgeBuilderCompatibilityTest.class.getClassLoader().getResourceAsStream(source + ".class")) {
            assertNotNull(input);
            new ClassReader(input).accept(new ClassRemapper(node, new SimpleRemapper(source, target)), 0);
        }
        return node;
    }

    static Class<?> transformedBuilder() throws Exception {
        final ClassNode target = read(BUILDER, GENERATED);
        final ClassNode mixin = read(MIXIN, GENERATED);
        target.innerClasses.clear();
        target.nestHostClass = null;
        target.nestMembers = null;
        target.interfaces.add(CompactQuadBuilderAccess.class.getName().replace('.', '/'));
        target.fields.stream().filter(field -> field.name.equals("unpackedData"))
                     .forEach(field -> field.access &= ~Opcodes.ACC_FINAL);
        mixin.fields.stream().filter(field -> field.name.startsWith("novaeng$")).forEach(target.fields::add);
        target.methods.removeIf(method -> method.name.equals("put"));
        mixin.methods.stream().filter(method -> method.name.startsWith("novaeng$") || method.name.equals("put"))
                     .forEach(target.methods::add);
        for (final MethodNode method : target.methods) {
            if (method.name.equals("<init>")) {
                int constants = 0;
                for (final var instruction : method.instructions.toArray()) {
                    if (instruction.getOpcode() == Opcodes.ICONST_4 && constants++ == 0) {
                        method.instructions.set(instruction, new InsnNode(Opcodes.ICONST_0));
                    }
                    if (instruction.getOpcode() == Opcodes.RETURN) {
                        final InsnList injection = new InsnList();
                        injection.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        injection.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        injection.add(new InsnNode(Opcodes.ACONST_NULL));
                        injection.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, GENERATED, "novaeng$allocateFlat",
                            "(Lnet/minecraft/client/renderer/vertex/VertexFormat;Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V", false));
                        method.instructions.insertBefore(instruction, injection);
                    }
                }
                assertEquals(2, constants, "Mapped Forge constructor array dimensions changed");
            } else if (method.name.equals("build")) {
                final InsnList injection = new InsnList();
                injection.add(new VarInsnNode(Opcodes.ALOAD, 0));
                injection.add(new InsnNode(Opcodes.ACONST_NULL));
                injection.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, GENERATED, "novaeng$exposeOriginalTree",
                    "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V", false));
                method.instructions.insert(injection);
            }
        }
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        target.accept(writer);
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(ForgeBuilderCompatibilityTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    private static UnpackedBakedQuad build(final Object builder) throws Exception {
        return (UnpackedBakedQuad) builder.getClass().getMethod("build").invoke(builder);
    }

    private static void fill(final IVertexConsumer builder, final int seed) {
        for (int v = 0; v < 4; v++)
            for (int e = 0; e < builder.getVertexFormat().getElementCount(); e++) {
                builder.put(e, v * 0.125f, e * 0.25f, Float.intBitsToFloat(0x7fc00000 + seed), -0.0f);
            }
    }

    @Test
    void publicBuildRetainsActualQuadTypeTreeAliasesAndLazyPacking() throws Exception {
        final Class<?> type = transformedBuilder();
        final VertexFormat format = DefaultVertexFormats.ITEM;
        final IVertexConsumer optimized = (IVertexConsumer) type.getConstructor(VertexFormat.class).newInstance(format);
        final var treeField = type.getDeclaredField("unpackedData");
        treeField.setAccessible(true);
        assertEquals(0, ((float[][][]) treeField.get(optimized)).length);
        final var buildFailure = assertThrows(InvocationTargetException.class, () -> build(optimized));
        assertEquals("not enough data", buildFailure.getCause().getMessage());
        fill(optimized, 42);
        final var textureFailure = assertThrows(InvocationTargetException.class, () -> build(optimized));
        assertEquals("texture not set", textureFailure.getCause().getMessage());
        optimized.setTexture(new LosslessFloatRecordTest.Sprite());
        final UnpackedBakedQuad first = build(optimized);
        final UnpackedBakedQuad second = build(optimized);
        assertEquals(UnpackedBakedQuad.class, first.getClass());
        assertSame(LosslessFloatRecordTest.tree(first), LosslessFloatRecordTest.tree(second));
        assertSame(treeField.get(optimized), LosslessFloatRecordTest.tree(first));
        assertSame(treeField.get(optimized), treeField.get(optimized));
        final List<float[]> arrays = new ArrayList<>();
        first.pipe(LosslessFloatRecordTest.consumer(format, arrays));
        arrays.getFirst()[0] = 0.875f;
        assertEquals(Float.floatToRawIntBits(0.875f), first.getVertexData()[0]);
        arrays.getFirst()[0] = 0.625f;
        assertEquals(Float.floatToRawIntBits(0.625f), second.getVertexData()[0]);
        assertEquals(Float.floatToRawIntBits(0.875f), first.getVertexData()[0]);
        first.vertexData[0] = 123;
        assertEquals(123, first.getVertexData()[0]);
        assertEquals(0.625f, arrays.getFirst()[0]);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> optimized.put(0, 1));
    }

    @Test
    void trustedSnapshotsAndSubsequentPublicBuildMatchUnmodifiedForge() throws Exception {
        final Class<?> type = transformedBuilder();
        final VertexFormat format = DefaultVertexFormats.ITEM;
        final TextureAtlasSprite sprite = new LosslessFloatRecordTest.Sprite();
        final IVertexConsumer optimized = (IVertexConsumer) type.getConstructor(VertexFormat.class).newInstance(format);
        final UnpackedBakedQuad.Builder original = new UnpackedBakedQuad.Builder(format);
        optimized.setTexture(sprite);
        original.setTexture(sprite);
        original.setContractUVs(true);
        type.getMethod("setContractUVs", boolean.class).invoke(optimized, true);
        fill(optimized, 83);
        fill(original, 83);
        for (int i = 0; i < 3; i++) {
            final OwnedUnpackedQuadRecord record = ((CompactQuadBuilderAccess) optimized).novaeng$buildRecord();
            final UnpackedBakedQuad reference = original.build();
            LosslessFloatRecordTest.assertTreeEquals(LosslessFloatRecordTest.tree(reference), record.data().materialize());
            assertArrayEquals(reference.getVertexData(), record.packed());
        }
        final UnpackedBakedQuad actual = build(optimized);
        final UnpackedBakedQuad expected = original.build();
        LosslessFloatRecordTest.assertTreeEquals(LosslessFloatRecordTest.tree(expected), LosslessFloatRecordTest.tree(actual));
        assertArrayEquals(expected.getVertexData(), actual.getVertexData());
    }

    @Test
    void interleavedBuildersKeepIndependentWorkingStorage() throws Exception {
        final Class<?> type = transformedBuilder();
        final VertexFormat format = DefaultVertexFormats.ITEM;
        final IVertexConsumer first = (IVertexConsumer) type.getConstructor(VertexFormat.class).newInstance(format);
        final IVertexConsumer second = (IVertexConsumer) type.getConstructor(VertexFormat.class).newInstance(format);
        final TextureAtlasSprite sprite = new LosslessFloatRecordTest.Sprite();
        first.setTexture(sprite);
        second.setTexture(sprite);
        for (int v = 0; v < 4; v++)
            for (int e = 0; e < format.getElementCount(); e++) {
                first.put(e, v, e, 0.125f, -0.0f);
                second.put(e, 3 - v, e, 0.875f, 1f);
            }
        final OwnedUnpackedQuadRecord firstRecord = ((CompactQuadBuilderAccess) first).novaeng$buildRecord();
        final OwnedUnpackedQuadRecord secondRecord = ((CompactQuadBuilderAccess) second).novaeng$buildRecord();
        assertEquals(Float.floatToRawIntBits(0.125f), firstRecord.data().rawBits(2, 0, 2));
        assertEquals(Float.floatToRawIntBits(0.875f), secondRecord.data().rawBits(2, 0, 2));
        final UnpackedBakedQuad publicFirst = build(first);
        final UnpackedBakedQuad publicSecond = build(second);
        assertNotSame(LosslessFloatRecordTest.tree(publicFirst), LosslessFloatRecordTest.tree(publicSecond));
        assertArrayEquals(firstRecord.packed(), publicFirst.getVertexData());
        assertArrayEquals(secondRecord.packed(), publicSecond.getVertexData());
    }
}
