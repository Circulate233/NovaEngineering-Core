package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.testutil.IsolatedMixinTransformer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingCheckResultAllocationTest {
    private static Class<?> transformedResult() throws Exception {
        final String target = "hellfirepvp/modularmachinery/common/crafting/helper/RecipeCraftingContext$CraftingCheckResult";
        final String name = "github/kasuminova/novaeng/mixin/mmce/LazyCheckResultProbe";
        final byte[] transformed = IsolatedMixinTransformer.transform(new String[]{target}, "MixinCraftingCheckResultAllocation")[0];
        final ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(new ClassRemapper(node, new SimpleRemapper(target, name)), 0);
        // The dependency's SUCCESS/FAILURE anonymous subclasses belong to the original
        // class. Instance methods and constructors are exercised independently here.
        node.methods.removeIf(method -> method.name.equals("<clinit>"));
        for (final var method : node.methods) {
            if (!method.name.equals("<init>")) continue;
            for (final var instruction : method.instructions) {
                if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW) {
                    assertFalse(type.desc.equals("it/unimi/dsi/fastutil/objects/Object2IntOpenHashMap"));
                    assertFalse(type.desc.equals("java/lang/NullPointerException"), "No constructor redirect null guard");
                }
            }
        }
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return new ClassLoader(CraftingCheckResultAllocationTest.class.getClassLoader()) {
            Class<?> define() {
                final byte[] bytes = writer.toByteArray();
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    @Test
    void realConstructorInjectionDefersMapAndErrorPromotionPreservesBehavior() throws Exception {
        final Class<?> type = transformedResult();
        final Object first = type.getConstructor().newInstance();
        final Object second = type.getConstructor().newInstance();
        final var field = type.getDeclaredField("unlocErrorMessagesMap");
        field.setAccessible(true);
        final var add = type.getMethod("addError", String.class);
        final var override = type.getMethod("overrideError", String.class);
        final var firstError = type.getMethod("getFirstErrorMessage", String.class);
        final var messages = type.getMethod("getUnlocalizedErrorMessages");
        assertNull(field.get(first));
        assertEquals(true, type.getMethod("isSuccess").invoke(first));
        assertEquals("default", firstError.invoke(first, "default"));
        add.invoke(first, "");
        assertNull(field.get(first));
        add.invoke(first, "a");
        add.invoke(first, "a");
        assertNull(field.get(first), "Repeated single error stays inline");
        assertEquals(List.of("a"), messages.invoke(first));
        add.invoke(first, "b");
        assertNotNull(field.get(first));
        assertEquals(List.of("b", "a"), messages.invoke(first));
        assertEquals("b", firstError.invoke(first, "default"));
        assertEquals(true, type.getMethod("isFailure").invoke(first));
        assertNull(field.get(second), "Result instances do not share mutable error maps");
        final Object promoted = field.get(first);
        override.invoke(first, "c");
        assertSame(promoted, field.get(first), "Override reuses an existing promoted map");
        assertEquals(List.of("c"), messages.invoke(first));
        @SuppressWarnings("unchecked") final List<String> copy = (List<String>) messages.invoke(first);
        copy.clear();
        assertEquals(List.of("c"), messages.invoke(first));
        override.invoke(first, "");
        assertEquals(true, type.getMethod("isSuccess").invoke(first));
        assertEquals("default", firstError.invoke(first, "default"));
        assertTrue(((List<?>) messages.invoke(first)).isEmpty());
    }
}
