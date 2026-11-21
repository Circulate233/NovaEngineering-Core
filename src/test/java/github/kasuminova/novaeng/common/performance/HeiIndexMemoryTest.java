package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.mixin.jei.MixinRecipeMapDeduplication;
import github.kasuminova.novaeng.mixin.jei.MixinRecipeRegistryIndexMemory;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.collect.ListMultiMap;
import mezz.jei.collect.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class HeiIndexMemoryTest {
    @Test
    void primitiveIdRowsPreserveZeroSignedIdsAndDynamicUpdates() throws Exception {
        final var mixin = new MixinRecipeRegistryIndexMemory() {
        };
        final Method factory = MixinRecipeRegistryIndexMemory.class.getDeclaredMethod("nova$primitiveRecipeIds");
        factory.setAccessible(true);
        @SuppressWarnings("unchecked") final Table<Object, Long, IRecipeWrapper> table =
            (Table<Object, Long, IRecipeWrapper>) factory.invoke(mixin);
        final Object category = new Object();
        final IRecipeWrapper first = ingredients -> {
        };
        final IRecipeWrapper second = ingredients -> {
        };
        final long[] ids = {0, -1, Long.MIN_VALUE, Long.MAX_VALUE, 1024, -1024};
        for (final long id : ids) {
            assertNull(table.put(category, id, first));
            assertSame(first, table.get(category, id));
        }
        final Map<Long, IRecipeWrapper> row = table.getRow(category);
        assertInstanceOf(HeiRecipeIdMap.class, row);
        HeiIndexCapacity.trimRows(Map.of(category, row));
        assertSame(row, table.getRow(category));
        assertSame(first, table.put(category, 0L, second));
        assertSame(second, table.get(category, 0L));
        assertNull(table.get(category, 7L));
        assertNull(table.get(new Object(), 0L));
        assertNull(row.get(null));
        assertNull(row.get("wrong type"));
        assertNull(row.put(null, first));
        assertSame(first, row.get(null));
        assertEquals(ids.length + 1, row.size());
        assertSame(first, row.remove(null));
        assertEquals(ids.length, row.size());
    }

    @Test
    void trimmingKeepsIdentityRowsListsAndLiveViewsMutable() {
        final Object2ObjectOpenHashMap<Object, Map<?, ?>> rows = new Object2ObjectOpenHashMap<>(128);
        final Reference2ObjectOpenHashMap<Object, Object> identityRow = new Reference2ObjectOpenHashMap<>(128);
        final String firstKey = new String("equal");
        final String secondKey = new String("equal");
        final ObjectArrayList<Object> list = new ObjectArrayList<>(128);
        final Object value = new Object();
        list.add(value);
        identityRow.put(firstKey, list);
        identityRow.put(secondKey, value);
        rows.put(new Object(), identityRow);
        final var liveValues = identityRow.values();

        HeiIndexCapacity.trimRows(rows);

        assertEquals(1, list.elements().length);
        assertSame(list, identityRow.get(firstKey));
        assertSame(value, identityRow.get(secondKey));
        assertEquals(2, identityRow.size());
        list.add(value);
        assertEquals(2, list.size());
        identityRow.put(new Object(), value);
        assertEquals(3, liveValues.size());
        assertNotSame(firstKey, secondKey);
    }

    @Test
    void categoryListsStartAtOneAndAllListKindsStillAcceptLaterRecipes() throws Exception {
        final var mixin = new MixinRecipeMapDeduplication() {
        };
        final Method factory = MixinRecipeMapDeduplication.class.getDeclaredMethod("nova$smallCategoryLists");
        factory.setAccessible(true);
        @SuppressWarnings("unchecked") final ListMultiMap<String, String> map = (ListMultiMap<String, String>) factory.invoke(mixin);
        map.put("ingredient", "first");
        final ObjectArrayList<?> list = assertInstanceOf(ObjectArrayList.class, map.get("ingredient"));
        assertEquals(1, list.elements().length);
        map.put("ingredient", "second");
        HeiIndexCapacity.trimMultiMap(map);
        assertEquals(List.of("first", "second"), map.get("ingredient"));
        map.put("ingredient", "third");
        assertEquals(List.of("first", "second", "third"), map.get("ingredient"));

        final ListMultiMap<String, String> standard = new ListMultiMap<>();
        standard.put("key", "value");
        final List<String> original = standard.get("key");
        assertInstanceOf(ArrayList.class, original);
        HeiIndexCapacity.trimMultiMap(standard);
        assertSame(original, standard.get("key"));
        standard.put("key", "later");
        assertEquals(List.of("value", "later"), original);
    }

    @Test
    void primitiveRowsMatchBoxedMapsForNullRawKeysAndLiveViewMutations() {
        final Map<Object, Object> baseline = new Object2ObjectOpenHashMap<>();
        final HeiRecipeIdMap<Object> optimized = new HeiRecipeIdMap<>();
        final var keysView = optimized.keySet();
        final var entriesView = optimized.entrySet();
        final var valuesView = optimized.values();
        final Object[] keys = {null, 0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 1024L, "raw", 7, new Object()};
        final Object[] values = {null, new Object(), new Object(), new Object()};
        final Random random = new Random(0x484549L);
        for (int i = 0; i < 1500; i++) {
            final Object key = keys[random.nextInt(keys.length)];
            final Object value = values[random.nextInt(values.length)];
            switch (random.nextInt(7)) {
                case 0 -> assertEquals(baseline.put(key, value), optimized.put(key, value));
                case 1 -> assertEquals(baseline.remove(key), optimized.remove(key));
                case 2 -> assertEquals(baseline.remove(key, value), optimized.remove(key, value));
                case 3 -> assertEquals(baseline.computeIfAbsent(key, ignored -> value),
                    optimized.computeIfAbsent(key, ignored -> value));
                case 4 -> assertEquals(baseline.keySet().remove(key), keysView.remove(key));
                case 5 -> {
                    final var entry = new AbstractMap.SimpleEntry<>(key, value);
                    assertEquals(baseline.entrySet().remove(entry), entriesView.remove(entry));
                }
                case 6 -> {
                    for (final var entry : baseline.entrySet())
                        if (Objects.equals(key, entry.getKey())) entry.setValue(value);
                    for (final var entry : entriesView)
                        if (Objects.equals(key, entry.getKey())) entry.setValue(value);
                }
            }
            assertEquals(baseline, optimized);
            assertEquals(optimized, baseline);
            assertEquals(baseline.keySet(), keysView);
            assertEquals(baseline.size(), valuesView.size());
            assertEquals(baseline.entrySet(), entriesView);
            if (i % 31 == 0) optimized.trim();
        }
        entriesView.clear();
        assertEquals(0, keysView.size());
        assertEquals(0, valuesView.size());
    }
}
