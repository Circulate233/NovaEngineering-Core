package github.kasuminova.novaeng.mixin.mmce;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class BlockInformationIngredientsTest {
    private static Class<?> implementation() throws Exception {
        // Execute the actual mixin methods with conversion hooks, without bootstrapping a client/world.
        final Map<String, String> names = new Object2ObjectOpenHashMap<>();
        names.put("net/minecraft/item/ItemStack", internal(Stack.class));
        names.put("net/minecraft/block/state/IBlockState", internal(State.class));
        names.put("net/minecraft/util/math/BlockPos", internal(Position.class));
        names.put("net/minecraft/world/World", internal(World.class));
        names.put("ink/ikx/mmce/common/utils/StackUtils", internal(Hooks.class));
        names.put("hellfirepvp/modularmachinery/common/util/ItemUtils", internal(Hooks.class));
        final ClassNode node = new ClassNode();
        try (final var input = BlockInformationIngredientsTest.class.getResourceAsStream("MixinBlockInformationIngredients.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(new ClassRemapper(node, new SimpleRemapper(names)), 0);
        }
        node.access &= ~Opcodes.ACC_ABSTRACT;
        final ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        final byte[] bytes = writer.toByteArray();
        return new ClassLoader(BlockInformationIngredientsTest.class.getClassLoader()) {
            Class<?> define() {
                return defineClass(null, bytes, 0, bytes.length);
            }
        }.define();
    }

    private static String internal(final Class<?> type) {
        return type.getName().replace('.', '/');
    }

    @Test
    void alternativesKeepOrderDeduplicationAndIndependentMutableResults() throws Exception {
        final Class<?> type = implementation();
        final Object instance = type.getConstructor().newInstance();
        final var samples = type.getDeclaredField("samples");
        samples.setAccessible(true);
        final List<State> states = new ObjectArrayList<>(List.of(new State("a"), new State("b"), new State("a")));
        samples.set(instance, states);
        final var ingredients = type.getMethod("getIngredientList");
        final List<?> first = (List<?>) ingredients.invoke(instance);
        assertEquals(List.of(new Stack("a"), new Stack("b")), first);
        first.clear();
        final List<?> second = (List<?>) ingredients.invoke(instance);
        assertEquals(List.of(new Stack("a"), new Stack("b")), second);
        assertNotSame(first, second);
        assertNotSame(second.getFirst(), ((List<?>) ingredients.invoke(instance)).getFirst());
        states.addFirst(new State("c"));
        assertEquals(List.of(new Stack("c"), new Stack("a"), new Stack("b")), ingredients.invoke(instance));
        states.clear();
        assertEquals(List.of(), ingredients.invoke(instance));
    }

    @Test
    void worldChangesAreObservedOnEveryGuiCall() throws Exception {
        final Class<?> type = implementation();
        final Object instance = type.getConstructor().newInstance();
        final var samples = type.getDeclaredField("samples");
        samples.setAccessible(true);
        samples.set(instance, List.of(new State("a"), new State("a"), new State("b")));
        final var ingredients = type.getMethod("getIngredientList", Position.class, World.class);
        final World world = new World();
        final Position pos = new Position();
        assertEquals(List.of(new Stack("afirst"), new Stack("bfirst")), ingredients.invoke(instance, pos, world));
        world.suffix = "changed";
        assertEquals(List.of(new Stack("achanged"), new Stack("bchanged")), ingredients.invoke(instance, pos, world));
    }

    public record Stack(String value) {
    }

    public record State(String value) {
    }

    public static final class Position {
    }

    public static final class World {
        public String suffix = "first";
    }

    public static final class Hooks {
        public static Stack getStackFromBlockState(final State state) {
            return new Stack(state.value());
        }

        public static Stack getStackFromBlockState(final State state, final Position pos, final World world) {
            assertNotNull(pos);
            return new Stack(state.value() + world.suffix);
        }

        public static boolean stackNotInList(final List<Stack> stacks, final Stack stack) {
            return !stacks.contains(stack);
        }
    }
}
