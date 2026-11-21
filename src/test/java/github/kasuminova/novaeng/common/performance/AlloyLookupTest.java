package github.kasuminova.novaeng.common.performance;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlloyLookupTest {
    // Avoid bootstrapping the entire game just to provide TriItemLookup's empty-slot sentinel.
    public static final class TestItems {
        public static final Item AIR = new Item();
    }

    @Test
    void multisetIndexMatchesOriginalPermutationTree() throws Exception {
        final Item[] items = {TestItems.AIR, new Item(), new Item(), new Item(), new Item()};
        for (int i = 0; i < items.length; i++) {
            Item.REGISTRY.register(1000 + i, new ResourceLocation("nova_startup_test", "item" + i), items[i]);
        }
        final ClassLoader loader = lookupLoader();
        final Class<?> baselineType = loader.loadClass("crazypants.enderio.base.recipe.lookup.TriItemLookup");
        final Class<?> optimizedType = loader.loadClass(UnorderedAlloyRecipeLookup.class.getName());
        final Object baseline = baselineType.getConstructor().newInstance();
        final Object optimized = optimizedType.getConstructor().newInstance();
        final Method one = baselineType.getMethod("addRecipe", Object.class, Item.class);
        final Method two = baselineType.getMethod("addRecipe", Object.class, Item.class, Item.class);
        final Method three = baselineType.getMethod("addRecipe", Object.class, Item.class, Item.class, Item.class);
        final Method unorderedTwo = optimizedType.getMethod("addUnordered", Object.class, Item.class, Item.class);
        final Method unorderedThree = optimizedType.getMethod("addUnordered", Object.class, Item.class, Item.class, Item.class);
        final Method query = baselineType.getMethod("getRecipes3", Item.class, Item.class, Item.class);
        final Random random = new Random(1896);
        for (int iteration = 0; iteration < 160; iteration++) {
            final Integer recipe = iteration % 41; // Equal recipes can appear in multiple alternative tuples.
            final Item a = items[1 + random.nextInt(items.length - 1)];
            final Item b = items[random.nextInt(items.length)];
            final Item c = items[random.nextInt(items.length)];
            switch (iteration % 3) {
                case 0 -> {
                    one.invoke(baseline, recipe, a);
                    one.invoke(optimized, recipe, a);
                }
                case 1 -> {
                    two.invoke(baseline, recipe, a, b);
                    two.invoke(baseline, recipe, b, a);
                    unorderedTwo.invoke(optimized, recipe, a, b);
                }
                default -> {
                    for (final Item[] permutation : new Item[][]{{a,b,c},{a,c,b},{b,a,c},{b,c,a},{c,a,b},{c,b,a}}) {
                        three.invoke(baseline, recipe, permutation[0], permutation[1], permutation[2]);
                    }
                    unorderedThree.invoke(optimized, recipe, a, b, c);
                }
            }
            for (final Item x : items) for (final Item y : items) for (final Item z : items) {
                assertEquals(query.invoke(baseline, x, y, z), query.invoke(optimized, x, y, z),
                    "candidate order after recipe " + iteration);
            }
            assertEquals(contents((Iterable<?>) baseline), contents((Iterable<?>) optimized), "rebuild iteration order");
        }
    }

    private static List<?> contents(final Iterable<?> lookup) {
        final List<Object> result = new ArrayList<>();
        lookup.forEach(result::add);
        return result;
    }

    private static ClassLoader lookupLoader() throws Exception {
        final Map<String, String> methods = new HashMap<>();
        final Map<String, String> fields = new HashMap<>();
        for (final String line : Files.readAllLines(Path.of("build/moddev/artifacts/intermediateToNamed.srg"))) {
            final String[] parts = line.split(" ");
            if (line.startsWith("MD: ")) methods.put(parts[1] + parts[2], parts[3].substring(parts[3].lastIndexOf('/') + 1));
            if (line.startsWith("FD: ")) fields.put(parts[1], parts[2].substring(parts[2].lastIndexOf('/') + 1));
        }
        final Remapper mappings = new Remapper() {
            @Override public String mapMethodName(String owner, String name, String descriptor) {
                return methods.getOrDefault(owner + '/' + name + descriptor, name);
            }
            @Override public String mapFieldName(String owner, String name, String descriptor) {
                return fields.getOrDefault(owner + '/' + name, name);
            }
            @Override public String map(String name) {
                return name.equals("net/minecraft/init/Items") ? TestItems.class.getName().replace('.', '/') : name;
            }
        };
        return new ClassLoader(AlloyLookupTest.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.startsWith("crazypants.enderio.base.recipe.lookup.") && !name.equals(UnorderedAlloyRecipeLookup.class.getName())) {
                    return super.loadClass(name, resolve);
                }
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    try (final var in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                        final ClassWriter out = new ClassWriter(0);
                        new ClassReader(in.readAllBytes()).accept(new ClassRemapper(out, mappings), 0);
                        final byte[] bytes = out.toByteArray();
                        type = defineClass(name, bytes, 0, bytes.length);
                    } catch (Exception failure) {
                        throw new ClassNotFoundException(name, failure);
                    }
                }
                if (resolve) resolveClass(type);
                return type;
            }
        };
    }
}
