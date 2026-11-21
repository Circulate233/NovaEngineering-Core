package github.kasuminova.novaeng.client.memory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.util.Annotations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks actual dependency bytecode without initializing Minecraft, OpenGL, or optional mods.
 */
class RenderBufferMixinTargetsTest {
    private static final String ROOT = "github/kasuminova/novaeng/mixin/";
    private static final String TESSELLATOR = "net/minecraft/client/renderer/Tessellator";
    private static final String TEXTURE_UTIL = "net/minecraft/client/renderer/texture/TextureUtil";
    private static final String CLS = "alexiil/mc/mod/load/render/TextureAnimator";

    private static ClassNode mixin(final String suffix, final String target) throws Exception {
        final ClassNode mixin = read(ROOT + suffix, false);
        AnnotationNode annotation = Annotations.getInvisible(mixin, Mixin.class);
        if (annotation == null) annotation = Annotations.getVisible(mixin, Mixin.class);
        assertNotNull(annotation, mixin.name);
        final List<Type> values = Annotations.getValue(annotation, "value");
        final List<String> targets = Annotations.getValue(annotation, "targets");
        assertEquals(List.of(target), values != null ? values.stream().map(Type::getInternalName).toList()
            : targets.stream().map(name -> name.replace('.', '/')).toList());
        return mixin;
    }

    private static ClassNode dependency(final String name) throws Exception {
        return read(name, true);
    }

    private static ClassNode read(final String name, final boolean dependency) throws Exception {
        final var resource = RenderBufferMixinTargetsTest.class.getClassLoader().getResource(name + ".class");
        assertNotNull(resource, "Missing declared dependency or compiled mixin: " + name);
        if (dependency) assertEquals("jar", resource.getProtocol(), "Read the actual dependency JAR: " + resource);
        try (final var input = resource.openStream()) {
            final ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static MemberInfo mapped(final ClassNode mixin, final String selector, final boolean required) throws Exception {
        final JsonObject mappings = JsonParser.parseString(Files.readString(
            Path.of(System.getProperty("novaeng.test.refmap")))).getAsJsonObject().getAsJsonObject("mappings");
        final JsonObject entries = mappings.getAsJsonObject(mixin.name);
        final var value = entries == null ? null : entries.get(selector);
        if (required) assertNotNull(value, "Missing generated mapping: " + mixin.name + " -> " + selector);
        return MemberInfo.parse(value == null ? selector : value.getAsString(), null).validate();
    }

    private static MethodNode targetMethod(final ClassNode type, final String named,
                                           final MemberInfo mapped, final String desc) {
        final List<MethodNode> matches = type.methods.stream().filter(method -> method.desc.equals(desc)
            && (method.name.equals(named) || mapped.matches(type.name, method.name, method.desc).isExactMatch())).toList();
        assertEquals(1, matches.size(), type.name + " -> " + named + desc);
        return matches.getFirst();
    }

    private static MethodNode method(final ClassNode type, final String name, final String desc) {
        return targetMethod(type, name, MemberInfo.parse(name + desc, null).validate(), desc);
    }

    private static void assertStatic(final MethodNode method, final boolean expected) {
        assertEquals(expected, (method.access & Opcodes.ACC_STATIC) != 0, method.name + method.desc);
    }

    private static boolean matches(final String selector, final MethodInsnNode call) {
        return MemberInfo.parse(selector, null).validate().matches(call.owner, call.name, call.desc).isExactMatch();
    }

    private static int calls(final MethodNode method, final String... selectors) {
        int count = 0;
        for (final var instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                && Arrays.stream(selectors).anyMatch(selector -> matches(selector, call))) count++;
        }
        return count;
    }

    private static int largeConstants(final MethodNode method) {
        int count = 0;
        for (final var instruction : method.instructions) {
            if (instruction instanceof LdcInsnNode ldc && Integer.valueOf(4194304).equals(ldc.cst)) count++;
        }
        return count;
    }

    @Test
    void capacityHooksMatchExactlyOneRealConstructorAndItsStaticness() throws Exception {
        for (final var spec : List.of(
            new ConstructorHook("minecraft/MixinGlobalTessellatorCapacity", TESSELLATOR,
                "nova$smallGlobalBuffer", "<clinit>", true, TESSELLATOR),
            new ConstructorHook("minecraft/MixinTesrTessellatorCapacity",
                "net/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher",
                "nova$smallTesrBuffer", "<init>", false, TESSELLATOR),
            new ConstructorHook("neofontrender/MixinCosmicTessellatorCapacity",
                "neofontrender/core/font/cosmic/CosmicTextRenderer",
                "nova$smallFontBuffer", "<clinit>", true, TESSELLATOR),
            new ConstructorHook("astralsorcery/MixinBufferBatchCapacity",
                "hellfirepvp/astralsorcery/client/util/BufferBatch",
                "nova$smallInitialBuffer", "<init>", false,
                "net/minecraft/client/renderer/BufferBuilder"))) {
            final ClassNode mixin = mixin(spec.mixin, spec.target);
            final MethodNode hook = method(mixin, spec.handler, "(I)I");
            final AnnotationNode injection = Annotations.getVisible(hook, ModifyArg.class);
            assertNotNull(injection, spec.mixin);
            assertEquals(1, (int) Annotations.<Integer>getValue(injection, "require"));
            assertEquals(0, (int) Annotations.<Integer>getValue(injection, "index"));
            assertEquals(List.of(spec.method), Annotations.<List<String>>getValue(injection, "method"));
            assertStatic(hook, spec.isStatic);
            final AnnotationNode at = Annotations.getValue(injection, "at");
            assertEquals("INVOKE", Annotations.<String>getValue(at, "value"));
            final String selector = Annotations.getValue(at, "target");
            assertEquals("L" + spec.owner + ";<init>(I)V", selector);
            final MemberInfo mapped = mapped(mixin, selector, false);
            assertEquals(selector, mapped.toString(), "Constructors keep their name and owner in SRG");
            final MethodNode constructor = method(dependency(spec.target), spec.method, "()V");
            assertStatic(constructor, spec.isStatic);
            assertEquals(1, calls(constructor, selector, mapped.toString()), spec.mixin);
            for (final var instruction : constructor.instructions) {
                if (instruction instanceof MethodInsnNode call && matches(selector, call)) {
                    assertEquals(Opcodes.INVOKESPECIAL, call.getOpcode());
                    var argument = call.getPrevious();
                    while (argument != null && argument.getOpcode() < 0) argument = argument.getPrevious();
                    assertInstanceOf(LdcInsnNode.class, argument);
                    assertEquals(2097152, ((LdcInsnNode) argument).cst,
                        "The selected argument must be the original large allocation");
                }
            }
        }
    }

    @Test
    void textureUtilReplacesAllThreeLargeConstantsWithCorrectCapturedArguments() throws Exception {
        final ClassNode mixin = mixin("minecraft/MixinTextureUploadCapacity", TEXTURE_UTIL);
        final ClassNode target = dependency(TEXTURE_UTIL);
        int matched = 0;
        for (final var spec : List.of(
            new ConstantHook("nova$smallUploadBuffer", "<clinit>", "()V", "<clinit>"),
            new ConstantHook("nova$boundedArrayUpload", "uploadTextureSub", "(I[IIIIIZZZ)V", "func_147947_a"),
            new ConstantHook("nova$boundedImageUpload", "uploadTextureImageSubImpl",
                "(Ljava/awt/image/BufferedImage;IIZZ)V", "func_110993_a"))) {
            final String args = "I" + spec.desc.substring(1, spec.desc.indexOf(')'));
            final MethodNode hook = method(mixin, spec.handler, "(" + args + ")I");
            final AnnotationNode injection = Annotations.getVisible(hook, ModifyConstant.class);
            assertNotNull(injection);
            assertEquals(List.of(spec.selector), Annotations.<List<String>>getValue(injection, "method"));
            assertEquals(1, (int) Annotations.<Integer>getValue(injection, "require"));
            final List<AnnotationNode> constants = Annotations.getValue(injection, "constant");
            assertEquals(1, constants.size());
            assertEquals(4194304, (int) Annotations.<Integer>getValue(constants.getFirst(), "intValue"));
            final MemberInfo remapped = mapped(mixin, spec.selector, !spec.selector.startsWith("<"));
            assertEquals(spec.srg, remapped.getName());
            if (!spec.selector.startsWith("<")) {
                assertEquals(TEXTURE_UTIL, remapped.getOwner());
                assertEquals(spec.desc, remapped.getDesc());
            }
            final MethodNode original = targetMethod(target, spec.selector, remapped, spec.desc);
            assertStatic(hook, true);
            assertStatic(original, true);
            assertEquals(1, largeConstants(original), spec.selector);
            matched += largeConstants(original);
        }
        assertEquals(3, matched);
        assertEquals(3, target.methods.stream().mapToInt(RenderBufferMixinTargetsTest::largeConstants).sum(),
            "A new large upload constant needs a corresponding capacity hook");
    }

    @Test
    void clsRedirectAndOverwriteMatchTheRealBufferCreationAndUploadApi() throws Exception {
        final ClassNode mixin = mixin("customloadingscreen/MixinTextureAnimatorUploadMemory", CLS);
        final ClassNode target = dependency(CLS);
        final MethodNode hook = method(mixin, "nova$deferUploadStorage", "(I)Ljava/nio/IntBuffer;");
        final AnnotationNode redirect = Annotations.getVisible(hook, Redirect.class);
        assertNotNull(redirect);
        assertEquals(1, (int) Annotations.<Integer>getValue(redirect, "require"));
        assertEquals(List.of("<clinit>"), Annotations.<List<String>>getValue(redirect, "method"));
        final AnnotationNode at = Annotations.getValue(redirect, "at");
        assertEquals("INVOKE", Annotations.<String>getValue(at, "value"));
        assertTrue(Annotations.<Boolean>getValue(at, "remap"));
        final String selector = Annotations.getValue(at, "target");
        assertEquals("Lnet/minecraft/client/renderer/GLAllocation;createDirectIntBuffer(I)Ljava/nio/IntBuffer;", selector);
        final MemberInfo remapped = mapped(mixin, selector, true);
        assertEquals("func_74527_f", remapped.getName());
        assertEquals("net/minecraft/client/renderer/GLAllocation", remapped.getOwner());
        assertEquals("(I)Ljava/nio/IntBuffer;", remapped.getDesc());
        final MethodNode clinit = method(target, "<clinit>", "()V");
        assertStatic(hook, true);
        assertStatic(clinit, true);
        assertEquals(1, calls(clinit, selector, remapped.toString()));
        assertEquals(1, largeConstants(clinit));

        final String uploadDesc = "(ILjava/awt/image/BufferedImage;)V";
        final MethodNode replacement = method(mixin, "uploadTextureImage", uploadDesc);
        final MethodNode original = method(target, "uploadTextureImage", uploadDesc);
        assertNotNull(Annotations.getVisible(replacement, Overwrite.class));
        assertStatic(replacement, true);
        assertStatic(original, true);
        assertEquals(Opcodes.ACC_PUBLIC, original.access & Opcodes.ACC_PUBLIC);
        assertEquals(Opcodes.ACC_PUBLIC, replacement.access & Opcodes.ACC_PUBLIC);
        for (final MethodNode upload : List.of(original, replacement)) {
            assertEquals(1, calls(upload, "L" + TEXTURE_UTIL + ";allocateTexture(III)V",
                "L" + TEXTURE_UTIL + ";func_110991_a(III)V"));
            assertEquals(4, calls(upload, "Lorg/lwjgl/opengl/GL11;glTexParameteri(III)V"));
            assertEquals(1, calls(upload,
                "Lorg/lwjgl/opengl/GL11;glTexSubImage2D(IIIIIIIILjava/nio/IntBuffer;)V"));
        }
        final MethodNode release = method(mixin, "nova$releaseUploadStorage",
            "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V");
        final AnnotationNode injection = Annotations.getVisible(release, Inject.class);
        assertNotNull(injection);
        assertEquals(List.of("close"), Annotations.<List<String>>getValue(injection, "method"));
        final List<AnnotationNode> releaseAts = Annotations.getValue(injection, "at");
        assertEquals(1, releaseAts.size());
        assertEquals("RETURN", Annotations.<String>getValue(releaseAts.getFirst(), "value"));
        assertEquals(1, (int) Annotations.<Integer>getValue(injection, "require"));
        assertStatic(release, false);
        assertStatic(method(target, "close", "()V"), false);
    }

    private record ConstructorHook(String mixin, String target, String handler, String method,
                                   boolean isStatic, String owner) {
    }

    private record ConstantHook(String handler, String selector, String desc, String srg) {
    }
}
