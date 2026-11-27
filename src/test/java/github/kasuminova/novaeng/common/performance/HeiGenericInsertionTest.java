package github.kasuminova.novaeng.common.performance;

import com.sun.management.ThreadMXBean;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.collect.Table;
import mezz.jei.recipes.RecipeCategoryComparator;
import mezz.jei.recipes.RecipeMap;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Runs without launching Minecraft; baseline is the installed HEI private method, not a copied algorithm.
 */
public class HeiGenericInsertionTest {
    private static volatile Object escaped;

    public static void main(String[] args) throws Exception {
        List<List<String>> cases = new ObjectArrayList<>(List.of(
            List.of(), List.of("a"), Collections.singletonList(null),
            Arrays.asList(null, "a", "a", null), List.of("a", "b", "a", "c", "b"),
            List.of("Aa", "BB", "Aa", "AaAa", "BBBB"), List.of("null-id", "a", "null-id")));
        Random random = new Random(1896);
        for (int i = 0; i < 1000; i++) {
            List<String> values = new ObjectArrayList<>();
            for (int n = random.nextInt(40); n > 0; n--) {
                values.add(random.nextInt(5) == 0 ? null : "id" + random.nextInt(9));
            }
            cases.add(values);
        }
        for (int caseIndex = 0; caseIndex < cases.size(); caseIndex++) {
            final List<String> expanded = cases.get(caseIndex);
            for (boolean stateful : new boolean[]{false, true}) {
                Fixture baseline = new Fixture(expanded, stateful);
                Fixture optimized = new Fixture(expanded, stateful);
                for (int repeat = 0; repeat < 2; repeat++) {
                    baseline.insert(false);
                    optimized.insert(true);
                }
                equal(baseline.events, optimized.events);
                equal(baseline.rows(), optimized.rows());
                equal(baseline.categories(), optimized.categories());
                equal(baseline.failure, optimized.failure);
            }
        }
        // Callback exceptions must leave the same partial wrapper/category state.
        final List<String> failurePoints = List.of("expand", "uid:a", "category", "categoryUid");
        for (int index = 0; index < failurePoints.size(); index++) {
            final String failAt = failurePoints.get(index);
            Fixture baseline = new Fixture(List.of("a", "b"), false);
            Fixture optimized = new Fixture(List.of("a", "b"), false);
            baseline.failAt = optimized.failAt = failAt;
            baseline.insert(false);
            optimized.insert(true);
            equal(baseline.events, optimized.events);
            equal(baseline.rows(), optimized.rows());
            equal(baseline.categories(), optimized.categories());
            equal(baseline.failure, optimized.failure);
        }
        capacityOrder();
        System.out.println("PASS: " + cases.size() * 2 + " installed-bytecode replay cases; exceptions, identity/order, mutable detached lists");
        if (args.length > 0 && args[0].equals("--capacity")) capacityExperiment();
    }

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> {
            if (m.getDeclaringClass() == Object.class) return switch (m.getName()) {
                case "hashCode" -> System.identityHashCode(p);
                case "equals" -> p == a[0];
                case "toString" -> type.getSimpleName();
                default -> throw new AssertionError(m);
            };
            return handler.invoke(p, m, a);
        });
    }

    private static void capacityOrder() throws Exception {
        Method flatten = Class.forName("github.kasuminova.novaeng.mixin.jei.MixinRecipeRegistryInsertion")
                              .getDeclaredMethod("nova$flattenGroups", Map.class);
        flatten.setAccessible(true);
        for (int size = 0; size < 40; size++) {
            Map<Object, List<List<String>>> groups = new Reference2ObjectOpenHashMap<>();
            for (int i = 0; i < size; i++) {
                // Equal but nonidentical keys must remain distinct.
                Object key = new EqualType();
                groups.put(key, List.of(List.of("first"), Arrays.asList(null, "last")));
            }
            Map<Object, List<String>> baseline = new Reference2ObjectOpenHashMap<>();
            for (var entry : groups.entrySet()) {
                List<String> values = new ObjectArrayList<>();
                final List<List<String>> alternatives = entry.getValue();
                for (int index = 0; index < alternatives.size(); index++) values.addAll(alternatives.get(index));
                baseline.put(entry.getKey(), values);
            }
            Map<Object, List<String>> candidate = (Map<Object, List<String>>) flatten.invoke(null, groups);
            equal(new ObjectArrayList<>(baseline.keySet()), new ObjectArrayList<>(candidate.keySet()));
            equal(size, candidate.size());
            for (Object key : baseline.keySet()) {
                equal(baseline.get(key), candidate.get(key));
                candidate.get(key).clear();
                candidate.get(key).add("mutation");
                equal(Arrays.asList(null, "last"), groups.get(key).get(1));
            }
            // Empty and singleton maps remain writable identity maps, including null keys.
            Object newKey = new Object();
            candidate.put(newKey, new ObjectArrayList<>());
            candidate.put(null, new ObjectArrayList<>());
            equal(true, candidate.containsKey(newKey));
            equal(true, candidate.containsKey(null));
        }
        List<String> original = List.of("a");
        List<String> copy = IngredientGroups.flatten(List.of(original));
        copy.clear();
        copy.add("b");
        equal(List.of("a"), original);
        IngredientGroups.flatten(List.of()).add("mutable");
    }

    private static void capacityExperiment() {
        var bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        Object key = new Object();
        for (int round = 0; round < 5; round++) {
            for (int size = 0; size <= 1; size++) {
                for (boolean small : new boolean[]{false, true}) {
                    long before = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
                    long start = System.nanoTime();
                    int count = 200_000;
                    for (int i = 0; i < count; i++) {
                        Map<Object, Object> map = small ? new Reference2ObjectOpenHashMap<>(1) : new Reference2ObjectOpenHashMap<>();
                        if (size == 1) map.put(key, key);
                        escaped = map;
                    }
                    long elapsed = System.nanoTime() - start;
                    long allocated = bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - before;
                    System.out.printf("round=%d size=%d expected=%s bytes/op=%.1f ns/op=%.1f%n", round, size,
                        small ? "1" : "default", (double) allocated / count, (double) elapsed / count);
                }
            }
        }
    }

    @Test
    void matchesInstalledInsertion() throws Exception {
        main(new String[0]);
    }

    private static final class Fixture extends RecipeMap {
        final List<String> events = new ObjectArrayList<>();
        final List<String> expanded;
        final boolean stateful;
        final IIngredientHelper<String> helper;
        final IIngredientType<String> type = proxy(IIngredientType.class, (p, m, a) -> String.class);
        final IRecipeWrapper wrapper = proxy(IRecipeWrapper.class, (p, m, a) -> null);
        final IRecipeCategory<IRecipeWrapper> category;
        int uidCalls;
        String failAt;
        String failure;

        Fixture(List<String> expanded, boolean stateful) {
            super(new RecipeCategoryComparator(List.of()), null);
            this.expanded = expanded;
            this.stateful = stateful;
            helper = proxy(IIngredientHelper.class, (p, m, a) -> {
                if (m.getName().equals("expandSubtypes")) {
                    event("expand");
                    // Check the input really is detached and mutable, including the empty case.
                    ((List<String>) a[0]).add("helper mutation");
                    return this.expanded;
                }
                if (m.getName().equals("getUniqueId")) {
                    event("uid:" + a[0]);
                    uidCalls++;
                    return a[0].equals("null-id") ? null : a[0] + (this.stateful ? ":" + uidCalls : "");
                }
                throw new AssertionError(m);
            });
            category = proxy(IRecipeCategory.class, (p, m, a) -> {
                if (m.getName().equals("getUid")) {
                    event("categoryUid");
                    return "category";
                }
                throw new AssertionError(m);
            });
            IIngredientRegistry registry = proxy(IIngredientRegistry.class, (p, m, a) -> helper);
            try {
                Field field = RecipeMap.class.getDeclaredField("ingredientRegistry");
                field.setAccessible(true);
                field.set(this, registry);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        void event(String event) {
            events.add(event);
            if (event.equals(failAt)) throw new IllegalStateException(event);
        }

        @Override
        public <V> void addRecipeCategory(IRecipeCategory category, V ingredient, IIngredientHelper<V> helper) {
            event("category");
            super.addRecipeCategory(category, ingredient, helper);
        }

        void insert(boolean optimized) throws Exception {
            List<String> input = IngredientGroups.flatten(List.of(List.of("unexpanded")));
            try {
                if (optimized) {
                    event("helper");
                    var table = (Table<IRecipeCategory, String, List<IRecipeWrapper>>) field("recipeWrapperTable");
                    HeiRecipeInsertion.insert(this, wrapper, category, helper, table.getRow(category), input);
                } else {
                    event("helper");
                    Method method = RecipeMap.class.getDeclaredMethod("addRecipe", IRecipeWrapper.class,
                        IRecipeCategory.class, IIngredientType.class, List.class);
                    method.setAccessible(true);
                    method.invoke(this, wrapper, category, type, input);
                }
            } catch (Exception e) {
                Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
                failure = cause.getClass().getName() + ":" + cause.getMessage();
            }
        }

        Object field(String name) throws Exception {
            Field field = RecipeMap.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(this);
        }

        Map<String, Integer> rows() throws Exception {
            var table = (Table<IRecipeCategory, String, List<IRecipeWrapper>>) field("recipeWrapperTable");
            Map<String, Integer> result = new Object2ObjectOpenHashMap<>();
            table.getRow(category).forEach((id, wrappers) -> result.put(id, wrappers.size()));
            return result;
        }

        Map<String, List<String>> categories() throws Exception {
            // Inspect the backing map without adding missing keys through ListMultiMap.get.
            Object multi = field("categoryUidMap");
            Class<?> clazz = multi.getClass();
            while (clazz != null) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        return (Map<String, List<String>>) field.get(multi);
                    }
                }
                clazz = clazz.getSuperclass();
            }
            throw new AssertionError("No multimap backing map");
        }
    }

    private static final class EqualType implements IIngredientType<String> {
        @Override
        public Class<? extends String> getIngredientClass() {
            return String.class;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof EqualType;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
