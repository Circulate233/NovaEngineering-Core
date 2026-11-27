package github.kasuminova.novaeng.mixin.jei;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import mezz.jei.Internal;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.ingredients.IngredientRegistry;
import mezz.jei.ingredients.Ingredients;
import mezz.jei.recipes.RecipeRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Replays the installed HEI implementation without starting Minecraft or a Mixin runtime.
 */
public class HeiRecipeIdReplayTest {
    static void main(String[] args) throws Exception {
        replay();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void replay() throws Exception {
        List<String> events = new ObjectArrayList<>();
        int[] calls = {0};
        IIngredientType<String> strings = () -> String.class;
        IIngredientType<Long> numbers = () -> Long.class;
        IIngredientHelper stringHelper = helper("string", events, calls);
        IIngredientHelper numberHelper = helper("long", events, calls);
        ObjectLinkedOpenHashSet<IIngredientType<?>> types = new ObjectLinkedOpenHashSet<>(List.of(strings, numbers));
        // Skip unrelated game constructors; the installed jar uses SRG Minecraft method names,
        // while Gradle's test classpath contains development names. All methods under test are real.
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field unsafeField = unsafeType.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object allocator = unsafeField.get(null);
        Method allocate = unsafeType.getMethod("allocateInstance", Class.class);
        IngredientRegistry registry = (IngredientRegistry) allocate.invoke(allocator, IngredientRegistry.class);
        setField(IngredientRegistry.class, registry, "ingredientHelperMap",
            ImmutableMap.of(strings, stringHelper, numbers, numberHelper));
        setField(IngredientRegistry.class, registry, "ingredientTypeMap",
            ImmutableMap.of(String.class, strings, Long.class, numbers));
        setField(IngredientRegistry.class, registry, "craftableIngredientTypes", types);
        RecipeRegistry baseline = (RecipeRegistry) allocate.invoke(allocator, RecipeRegistry.class);
        setRegistry(RecipeRegistry.class, baseline, registry);
        Candidate candidate = new Candidate();
        setRegistry(MixinRecipeRegistryId.class, candidate, registry);
        Method original = method(RecipeRegistry.class, "calculateId", IRecipeCategory.class, Ingredients.class);
        Method replacement = method(MixinRecipeRegistryId.class, "calculateId", IRecipeCategory.class, Ingredients.class);
        IRecipeCategory category = (IRecipeCategory) Proxy.newProxyInstance(IRecipeCategory.class.getClassLoader(),
            new Class<?>[]{IRecipeCategory.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("getUid")) {
                    events.add("category");
                    return "nova:recipe-id-replay";
                }
                throw new AssertionError(method);
            });

        Field internalRegistry = Internal.class.getDeclaredField("ingredientRegistry");
        internalRegistry.setAccessible(true);
        IngredientRegistry previous = (IngredientRegistry) internalRegistry.get(null);
        internalRegistry.set(null, registry);
        int checked = 0;
        try {
            Random random = new Random(20261119);
            for (int trial = 0; trial < 400; trial++) {
                if ((trial & 1) != 0) {
                    types.clear();
                    types.add(numbers);
                    types.add(strings);
                } else {
                    types.clear();
                    types.add(strings);
                    types.add(numbers);
                }
                Ingredients ingredients = new Ingredients();
                for (IIngredientType type : types) {
                    // Include absent types, empty groups, duplicates, nulls, mixed registered classes,
                    // and iterator-only inner lists; never replace the actual installed Ingredients.
                    if (random.nextBoolean()) ingredients.setInputLists(type, groups(random));
                    if (random.nextBoolean()) ingredients.setOutputLists(type, groups(random));
                }
                for (boolean stateful : new boolean[]{false, true}) {
                    calls[0] = stateful ? 1 : 0;
                    events.clear();
                    Object expected = invoke(original, baseline, category, ingredients);
                    List<String> expectedEvents = new ObjectArrayList<>(events);
                    calls[0] = stateful ? 1 : 0;
                    events.clear();
                    Object actual = invoke(replacement, candidate, category, ingredients);
                    equal(expected, actual);
                    equal(expectedEvents, events);
                    checked++;
                }
            }
            // Each invocation must observe edits to the existing lists and a fresh hash callback.
            List<String> mutable = new ObjectArrayList<>(Arrays.asList("first", null, "first"));
            Ingredients ingredients = new Ingredients();
            ingredients.setInputLists(strings, List.of(mutable));
            for (int change = 0; change < 4; change++) {
                mutable.add("change" + change);
                events.clear();
                calls[0] = 1;
                Object expected = invoke(original, baseline, category, ingredients);
                List<String> expectedEvents = new ObjectArrayList<>(events);
                events.clear();
                calls[0] = 1;
                equal(expected, invoke(replacement, candidate, category, ingredients));
                equal(expectedEvents, events);
                checked++;
            }
        } finally {
            internalRegistry.set(null, previous);
        }
        System.out.println("PASS: " + checked + " installed-HEI recipe ID replays; ordered hash callbacks, live lists, both type orders");
    }

    private static List<List<Object>> groups(Random random) {
        List<List<Object>> groups = new ObjectArrayList<>();
        for (int count = random.nextInt(6); count > 0; count--) {
            List<Object> values = random.nextBoolean() ? new ObjectArrayList<>() : new LinkedList<>();
            for (int size = random.nextInt(10); size > 0; size--) {
                values.add(switch (random.nextInt(4)) {
                    case 0 -> null;
                    case 1 -> Long.valueOf(random.nextInt(5));
                    default -> "value" + random.nextInt(5);
                });
            }
            groups.add(values);
        }
        return groups;
    }

    @SuppressWarnings("rawtypes")
    private static IIngredientHelper helper(String name, List<String> events, int[] calls) {
        return (IIngredientHelper) Proxy.newProxyInstance(IIngredientHelper.class.getClassLoader(),
            new Class<?>[]{IIngredientHelper.class}, (proxy, method, args) -> switch (method.getName()) {
                case "expandSubtypes" -> args[0];
                case "getHash" -> {
                    events.add(name + ":" + args[0]);
                    int sequence = calls[0] == 0 ? 0 : calls[0]++;
                    yield args[0].hashCode() * 31 + sequence;
                }
                case "getFullUniqueId" -> name + ":" + args[0];
                default -> throw new AssertionError(method);
            });
    }

    private static Method method(Class<?> owner, String name, Class<?>... parameters) throws Exception {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static void setRegistry(Class<?> owner, Object instance, IngredientRegistry registry) throws Exception {
        setField(owner, instance, "ingredientRegistry", registry);
    }

    private static void setField(Class<?> owner, Object instance, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private static Object invoke(Method method, Object receiver, Object... args) throws Exception {
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException failure) {
            throw new AssertionError("Recipe ID replay failed", failure.getCause());
        }
    }

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }

    @Test
    void matchesInstalledRecipeIds() throws Exception {
        replay();
    }

    private static final class Candidate extends MixinRecipeRegistryId {
    }
}
