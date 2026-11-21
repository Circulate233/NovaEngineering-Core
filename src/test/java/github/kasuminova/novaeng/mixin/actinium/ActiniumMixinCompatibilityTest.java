package github.kasuminova.novaeng.mixin.actinium;

import com.google.gson.JsonParser;
import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Exercises the real Mixin and MixinExtras injectors against the configured Actinium dependency.
 */
class ActiniumMixinCompatibilityTest {
    private static final String MIXIN_PACKAGE = "github.kasuminova.novaeng.mixin";
    private static final Set<String> MINECRAFT_BRIDGES = Set.of(
        "minecraft.MixinDynamicTextureUploadBinding", "minecraft.MixinTextureMapUploadBinding",
        "minecraft.MixinFramebufferBlit", "minecraft.MixinRegionRenderCacheBuilderCapacity",
        "minecraft.MixinTileEntityRendererDistance", "minecraft.MixinEntityRendererTileEntityCulling");
    private static final Set<String> CALLBACK_FREE_TARGETS = Set.of(
        "com/dhj/actinium/render/FastLitItemDisplayListCache",
        "com/dhj/actinium/render/terrain/ActiniumWorldRenderer",
        "com/gtnewhorizons/angelica/glsm/GLSMCurrentAttribs",
        "com/gtnewhorizons/angelica/glsm/GLSMVertexInput",
        "com/gtnewhorizons/angelica/glsm/RenderSystem",
        "com/gtnewhorizons/angelica/glsm/GLSMAttribStack",
        "dhj/embeddedt/embeddium/impl/gl/device/GLRenderDevice$ImmediateCommandList",
        "com/mitchej123/glsm/impl/PassThroughGLStateManager");

    private static void assertRenderingHooksAvoidCallbackWrappers(final byte[] transformed) {
        final ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        final boolean callbackFree = CALLBACK_FREE_TARGETS.contains(node.name);
        for (final var method : node.methods) {
            // Initialization hooks may retain CallbackInfo; binding a context must keep its
            // primitive return value without allocating a CallbackInfoReturnable<Boolean>.
            final boolean contextHook = node.name.equals("com/gtnewhorizons/angelica/glsm/GLStateManager")
                && (method.name.equals("bindThreadContext") || method.name.contains("nova$invalidateAfterContextChange"));
            if (!callbackFree && !contextHook) continue;
            final String location = node.name + "." + method.name;
            assertFalse(method.desc.contains("org/spongepowered/asm/mixin/injection/callback/"), location);
            assertFalse(method.desc.contains("com/llamalad7/mixinextras/injector/wrapoperation/Operation"), location);
            for (final var instruction : method.instructions) {
                if (instruction instanceof TypeInsnNode type) {
                    assertFalse(type.desc.startsWith("org/spongepowered/asm/mixin/injection/callback/"), location);
                } else if (instruction instanceof MethodInsnNode call) {
                    assertFalse(call.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/"), location);
                    assertNotEquals("com/llamalad7/mixinextras/injector/wrapoperation/Operation", call.owner, location);
                } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                    assertFalse(dynamic.desc.contains("com/llamalad7/mixinextras/injector/wrapoperation/Operation"), location);
                }
            }
        }
    }

    private static ClassNode read(final String name) throws Exception {
        try (final var input = ActiniumMixinCompatibilityTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            assertNotNull(input, name);
            final ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    @TestFactory
    Stream<DynamicTest> registeredActiniumMixinsApplyToTheActualDependency() throws Exception {
        final var targets = new LinkedHashMap<String, List<String>>();
        try (final var stream = getClass().getResourceAsStream("/novaeng_core.mixin.json")) {
            assertNotNull(stream);
            final var config = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (final String side : List.of("mixins", "client", "server")) {
                for (final var entry : config.getAsJsonArray(side)) {
                    final String mixin = entry.getAsString();
                    if (!mixin.startsWith("actinium.") && !mixin.startsWith("geometry.actinium.")
                        && !MINECRAFT_BRIDGES.contains(mixin)) continue;
                    final ClassNode node = read(MIXIN_PACKAGE.replace('.', '/') + "/" + mixin.replace('.', '/'));
                    for (final AnnotationNode annotation : node.invisibleAnnotations) {
                        if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) continue;
                        for (int index = 0; index < annotation.values.size(); index += 2) {
                            if (!List.of("value", "targets").contains(annotation.values.get(index))) continue;
                            for (final Object value : (List<?>) annotation.values.get(index + 1)) {
                                final String target = value instanceof Type type ? type.getInternalName() : value.toString().replace('.', '/');
                                targets.computeIfAbsent(target, ignored -> new ArrayList<>()).add(mixin);
                            }
                        }
                    }
                }
            }
        }
        assertFalse(targets.isEmpty(), "The test must follow the actual registered compatibility mixins");
        return targets.entrySet().stream().map(entry -> DynamicTest.dynamicTest(entry.getKey(), () -> {
            final byte[] original;
            try (final var input = getClass().getClassLoader().getResourceAsStream(entry.getKey() + ".class")) {
                assertNotNull(input, "Missing configured target for " + entry.getValue());
                original = input.readAllBytes();
            }
            final byte[] transformed = IsolatedMixinTransformer.transformInPackage(MIXIN_PACKAGE,
                new String[]{entry.getKey()}, entry.getValue().toArray(String[]::new))[0];
            assertFalse(Arrays.equals(original, transformed), "Mixins were not applied: " + entry.getValue());
            assertRenderingHooksAvoidCallbackWrappers(transformed);
        }));
    }
}
