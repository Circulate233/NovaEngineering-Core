package github.kasuminova.novaeng.mixin.customloadingscreen;

import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomLoadingScreenRefreshTest {
    private List<MemberInfo> selectors() throws IOException {
        final List<MemberInfo> selectors = new ObjectArrayList<>();
        final ClassNode mixin = new ClassNode();
        try (InputStream input = MixinMinecraftDisplayerRenderer.class.getResourceAsStream("MixinMinecraftDisplayerRenderer.class")) {
            new ClassReader(input).accept(mixin, 0);
        }
        for (final MethodNode method : mixin.methods) {
            final AnnotationNode redirect = Annotations.getVisible(method, Redirect.class);
            if (redirect == null) {
                continue;
            }
            assertNull(Annotations.getInvisible(method, Group.class), "An unknown CLS layout must not prevent startup");
            assertEquals(0, (int) Annotations.<Integer>getValue(redirect, "require"));
            assertEquals(0, (int) Annotations.<Integer>getValue(redirect, "expect"));
            assertFalse(Annotations.<Boolean>getValue(redirect, "remap"));
            assertEquals(List.of("<init>"), Annotations.<List<String>>getValue(redirect, "method"));
            final AnnotationNode at = Annotations.getValue(redirect, "at");
            assertTrue(Annotations.<Boolean>getValue(at, "remap"), "Minecraft calls use the generated refmap");
            selectors.add(MemberInfo.parse(Annotations.getValue(at, "target"), null).validate());
        }
        assertEquals(1, selectors.size());
        return selectors;
    }

    @Test
    void namedSelectorIsRemappedAndOtherReloadsStayUntouched() throws IOException {
        final List<MemberInfo> selectors = this.selectors();
        assertEquals("refreshResources", selectors.getFirst().getName());
        final MemberInfo mapped = mappedSelector(selectors.getFirst());
        assertEquals("func_110436_a", mapped.getName());
        assertEquals(selectors.getFirst().getOwner(), mapped.getOwner());
        assertEquals(selectors.getFirst().getDesc(), mapped.getDesc());
        assertTrue(selectors.stream().noneMatch(selector ->
            selector.matches("net/minecraft/client/Minecraft", "func_110442_L",
                "()Lnet/minecraft/client/resources/IResourceManager;").isExactMatch()));
    }

    private MemberInfo mappedSelector(final MemberInfo selector) throws IOException {
        final Path refmap = Path.of(System.getProperty("novaeng.test.refmap"));
        final var mappings = JsonParser.parseString(Files.readString(refmap)).getAsJsonObject()
                                       .getAsJsonObject("mappings").getAsJsonObject(MixinMinecraftDisplayerRenderer.class.getName().replace('.', '/'));
        assertNotNull(mappings, "The annotation processor must generate the CLS mapping");
        final var mapped = mappings.get(selector.toString());
        assertNotNull(mapped, "The Minecraft call must have a generated mapping");
        return MemberInfo.parse(mapped.getAsString(), null).validate();
    }

    @Test
    void clsDependencyConstructorContainsExactlyOneMatchingCallAndKeepsLocalReloads() throws Exception {
        final ClassNode target = new ClassNode();
        try (InputStream input = getClass().getResourceAsStream(
            "/alexiil/mc/mod/load/render/MinecraftDisplayerRenderer.class")) {
            assertNotNull(input, "The declared CLS dependency must be available to this regression test");
            new ClassReader(input).accept(target, 0);
        }
        final List<MemberInfo> selectors = this.selectors();
        final List<MemberInfo> mapped = new ObjectArrayList<>();
        for (final MemberInfo selector : selectors) mapped.add(mappedSelector(selector));
        int refreshes = 0;
        int localReloads = 0;
        for (final MethodNode method : target.methods) {
            if (!method.name.equals("<init>")) {
                continue;
            }
            for (final AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    final boolean namedMatch = selectors.stream().anyMatch(selector ->
                        selector.matches(call.owner, call.name, call.desc).isExactMatch());
                    final boolean runtimeMatch = mapped.stream().anyMatch(selector ->
                        selector.matches(call.owner, call.name, call.desc).isExactMatch());
                    if (namedMatch || runtimeMatch) refreshes++;
                    if (call.name.equals("func_110549_a") || call.name.equals("onResourceManagerReload")) {
                        localReloads++;
                    }
                }
            }
        }
        assertEquals(1, refreshes);
        assertEquals(2, localReloads, "CLS texture manager and font must still reload locally");
    }
}
