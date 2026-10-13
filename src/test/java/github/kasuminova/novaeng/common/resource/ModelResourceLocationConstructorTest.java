package github.kasuminova.novaeng.common.resource;

import github.kasuminova.novaeng.common.asm.ModelResourceLocationConstructorPatch;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelResourceLocationConstructorTest {
    private static Class<?> transformedClass() throws Exception {
        final ClassNode target = new ClassNode();
        try (var input = ModelResourceLocation.class.getResourceAsStream("ModelResourceLocation.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(target, 0);
        }
        assertTrue(ModelResourceLocationConstructorPatch.apply(target));
        assertFalse(ModelResourceLocationConstructorPatch.apply(target));
        for (var method : target.methods) {
            if (method.name.equals("<init>") && method.desc.startsWith("(Lnet/minecraft/util/ResourceLocation;")) {
                new Analyzer<>(new BasicVerifier()).analyze(target.name, method);
                for (var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call) {
                        assertNotEquals("toString", call.name);
                    }
                }
            }
        }
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        target.accept(writer);
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(ModelResourceLocationConstructorTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(ModelResourceLocation.class.getName(), bytes, 0, bytes.length);
            }
        }.define();
    }

    @Test
    void transformedConstructorPreservesValuesAndReusesTheOriginalStrings() throws Exception {
        final Class<?> transformed = transformedClass();
        final ResourceLocation location = new ResourceLocation("novaeng", "models/test");
        final Object model = transformed.getConstructor(ResourceLocation.class, String.class).newInstance(location, "FACING=NORTH");
        assertSame(location.getNamespace(), transformed.getMethod("getNamespace").invoke(model));
        assertSame(location.getPath(), transformed.getMethod("getPath").invoke(model));
        assertEquals("facing=north", transformed.getMethod("getVariant").invoke(model));
    }

    @Test
    void matchesVanillaForSpecialPathsVariantsAndSubclasses() throws Exception {
        final Class<?> transformed = transformedClass();
        final ResourceLocation[] locations = {
            new ResourceLocation("nova", "test"), new ResourceLocation("x", "test"),
            new ResourceLocation("nova:odd", "test"), new ResourceLocation("nova", "test#old"),
            new ResourceLocation("nova", ""), new ModelResourceLocation("nova:test#old"),
            new ResourceLocation("nova", "test") {
                @Override
                public String toString() {
                    return "override:result#old";
                }
            }
        };
        for (ResourceLocation location : locations) {
            for (String variant : new String[]{null, "", "NORMAL", "a#b", "FACING=NORTH"}) {
                final ModelResourceLocation expected = new ModelResourceLocation(location, variant);
                final Object actual = transformed.getConstructor(ResourceLocation.class, String.class).newInstance(location, variant);
                assertEquals(expected.toString(), actual.toString());
                assertEquals(expected.hashCode(), actual.hashCode());
            }
        }
    }
}
