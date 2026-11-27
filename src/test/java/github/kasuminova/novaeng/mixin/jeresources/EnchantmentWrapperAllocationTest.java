package github.kasuminova.novaeng.mixin.jeresources;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class EnchantmentWrapperAllocationTest {
    private static Class<?> implementation() throws Exception {
        final Map<String, String> names = new Object2ObjectOpenHashMap<>();
        names.put("net/minecraft/item/ItemStack", internal(Stack.class));
        names.put("jeresources/entry/EnchantmentEntry", internal(Entry.class));
        names.put("jeresources/registry/EnchantmentRegistry", internal(Registry.class));
        names.put("jeresources/jei/enchantment/EnchantmentWrapper", internal(Wrapper.class));
        names.put("github/kasuminova/novaeng/mixin/jeresources/InvokerEnchantmentWrapper", internal(Invoker.class));
        final ClassNode node = new ClassNode();
        try (final var input = EnchantmentWrapperAllocationTest.class.getResourceAsStream("MixinEnchantmentWrapper.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(new ClassRemapper(node, new SimpleRemapper(names)), 0);
        }
        node.access &= ~Opcodes.ACC_ABSTRACT;
        final ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(EnchantmentWrapperAllocationTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    private static String internal(final Class<?> type) {
        return type.getName().replace('.', '/');
    }

    @Test
    void emptyAndChangingApplicabilityKeepIndependentOrderedSnapshots() throws Exception {
        final Class<?> type = implementation();
        final var create = type.getMethod("create", Stack.class);
        final Stack stack = new Stack();
        final Set<Entry> applicable = Registry.INSTANCE.applicable;
        applicable.clear();
        assertNull(create.invoke(null, stack));
        applicable.add(new Entry("two"));
        applicable.add(new Entry("one"));
        final Wrapper first = (Wrapper) create.invoke(null, stack);
        assertSame(stack, first.stack());
        assertEquals(Collections.unmodifiableList(new ObjectArrayList<>(applicable)), first.entries());
        applicable.clear();
        assertEquals(List.of(new Entry("two"), new Entry("one")), first.entries());
        assertNull(create.invoke(null, stack));
        applicable.add(new Entry("new"));
        final Wrapper second = (Wrapper) create.invoke(null, stack);
        assertEquals(List.of(new Entry("new")), second.entries());
        second.entries().clear();
        assertEquals(Set.of(new Entry("new")), applicable);
    }

    public interface Invoker {
        static Wrapper nova$create(final Stack stack, final List<Entry> entries) {
            return new Wrapper(stack, entries);
        }
    }

    public static final class Stack {
    }

    public record Entry(String name) {
    }

    public record Wrapper(Stack stack, List<Entry> entries) {
    }

    public static final class Registry {
        public static final Registry INSTANCE = new Registry();
        public final Set<Entry> applicable = new ObjectLinkedOpenHashSet<>();

        public static Registry getInstance() {
            return INSTANCE;
        }

        public Set<Entry> getEnchantments(final Stack stack) {
            return applicable;
        }
    }
}
