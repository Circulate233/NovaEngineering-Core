package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.LowerStringMap;
import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedAttributeNameMapTest {
    @Test
    void originalLowerStringMapMethodsKeepNormalizationAndWritableViews() throws Exception {
        final LowerStringMap<Integer> map = new AttributeNameIndex<>();
        final var storage = LowerStringMap.class.getDeclaredField("internalMap");
        storage.setAccessible(true);
        // Install exactly the backing object installed by the production storage
        // mixin; execute every original LowerStringMap method without rewriting it.
        storage.set(map, new SharedAttributeNameMap<Integer>());
        final var values = map.values();
        map.put("GeNeRiC.MaxHealth", 20);
        assertEquals(20, map.get("GENERIC.MAXHEALTH"));
        assertTrue(map.containsKey(new StringBuilder("generic.MAXHEALTH")));
        final Map<String, Integer> extra = new LinkedHashMap<>();
        extra.put("GENERIC.MAXHEALTH", 30);
        extra.put("Follow.Range", 16);
        map.putAll(extra);
        assertEquals(List.of(30, 16), new ObjectArrayList<>(values));
        // Vanilla's containsValue delegates containsKey; retain that existing
        // behavior instead of silently fixing an unrelated public API here.
        assertTrue(map.containsValue("generic.maxhealth"));
        assertFalse(map.containsValue(30));
        assertEquals(30, map.remove(new StringBuilder("GENERIC.MAXHEALTH")));
        assertEquals(List.of(16), new ObjectArrayList<>(values));
        map.put("GENERIC.MAXHEALTH", 40);
        assertEquals(List.of("follow.range", "generic.maxhealth"), new ObjectArrayList<>(map.keySet()));
        map.entrySet().iterator().next().setValue(24);
        assertEquals(24, map.get("FOLLOW.RANGE"));
        final var iterator = values.iterator();
        assertEquals(24, iterator.next());
        iterator.remove();
        assertFalse(map.containsKey("follow.range"));
        assertThrows(NullPointerException.class, () -> map.put(null, 1));
        assertThrows(NullPointerException.class, () -> map.get(null));
        values.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    void repeatedSequencesShareOnlyTheImmutableKeyLayout() throws Exception {
        final SharedAttributeNameMap<Integer> first = new SharedAttributeNameMap<>();
        final SharedAttributeNameMap<Integer> second = new SharedAttributeNameMap<>();
        for (int index = 0; index < 48; index++) {
            first.put("shared.name." + index, index);
            second.put("shared.name." + index, -index);
        }
        final var layout = SharedAttributeNameMap.class.getDeclaredField("layout");
        layout.setAccessible(true);
        assertSame(layout.get(first), layout.get(second));
        assertEquals(20, first.get("shared.name.20"));
        assertEquals(-20, second.get("shared.name.20"));
        assertEquals(48, new ObjectArrayList<>(first.values()).size());
        final var expanded = SharedAttributeNameMap.class.getDeclaredField("expanded");
        expanded.setAccessible(true);
        assertNull(expanded.get(first), "Ordinary getAllAttributes iteration must not create entry nodes");
        second.put("shared.name.20", null);
        assertTrue(second.containsKey("shared.name.20"));
        assertNull(second.get("shared.name.20"));
        assertEquals(20, first.get("shared.name.20"));
    }

    @Test
    void viewsRemainLiveWritableOrderedAndFailFastAcrossExpansion() {
        final SharedAttributeNameMap<Integer> actual = new SharedAttributeNameMap<>();
        actual.put("a", 1);
        actual.put("b", 2);
        actual.put("c", 3);
        final var values = actual.values();
        final Iterator<Integer> compact = values.iterator();
        assertEquals(1, compact.next());
        final var entries = actual.entrySet();
        final var first = entries.iterator().next();
        assertEquals(1, first.setValue(11));
        actual.put("b", 22);
        assertEquals(22, compact.next(), "Entry exposure and replacement do not structurally modify the map");
        compact.remove();
        assertEquals(3, compact.next());
        assertEquals(List.of(11, 3), new ObjectArrayList<>(values));
        actual.remove("a");
        first.setValue(99);
        assertFalse(actual.containsKey("a"), "A detached entry must not reinsert its key");
        actual.put("a", 4);
        assertEquals(List.of("c", "a"), new ObjectArrayList<>(actual.keySet()));
        final Iterator<Integer> invalidated = values.iterator();
        actual.put("d", 5);
        assertThrows(ConcurrentModificationException.class, invalidated::next);
        values.clear();
        assertTrue(entries.isEmpty());
    }

    @Test
    void oversizedLayoutsNullKeysAndReinsertionsRetainLinkedHashMapBehavior() {
        final SharedAttributeNameMap<Integer> actual = new SharedAttributeNameMap<>();
        final LinkedHashMap<String, Integer> expected = new LinkedHashMap<>();
        for (int index = 0; index < 160; index++) {
            actual.put("large." + index, index);
            expected.put("large." + index, index);
        }
        actual.put(null, null);
        expected.put(null, null);
        assertEquals(expected, actual);
        assertEquals(new ObjectArrayList<>(expected.values()), new ObjectArrayList<>(actual.values()));
        actual.remove("large.0");
        expected.remove("large.0");
        actual.put("large.0", -1);
        expected.put("large.0", -1);
        assertEquals(new ObjectArrayList<>(expected.keySet()), new ObjectArrayList<>(actual.keySet()));
    }

    @Test
    void randomMutationsMatchLinkedHashMap() {
        final Random random = new Random(0xA771B0);
        for (int sample = 0; sample < 80; sample++) {
            final Map<String, Integer> actual = new SharedAttributeNameMap<>();
            final Map<String, Integer> expected = new LinkedHashMap<>();
            for (int step = 0; step < 100; step++) {
                final String key = "random." + random.nextInt(12);
                final Integer value = random.nextBoolean() ? random.nextInt(20) : null;
                switch (random.nextInt(7)) {
                    case 0, 1 -> assertEquals(expected.put(key, value), actual.put(key, value));
                    case 2 -> assertEquals(expected.remove(key), actual.remove(key));
                    case 3 -> assertEquals(expected.values().remove(value), actual.values().remove(value));
                    case 4 -> assertEquals(expected.putIfAbsent(key, value), actual.putIfAbsent(key, value));
                    case 5 -> assertEquals(expected.compute(key, (name, previous) -> value),
                        actual.compute(key, (name, previous) -> value));
                    case 6 -> {
                        if (random.nextInt(10) == 0) {
                            expected.clear();
                            actual.clear();
                        }
                    }
                    default -> throw new AssertionError();
                }
                assertEquals(expected.size(), actual.size());
                assertEquals(new ObjectArrayList<>(expected.values()), new ObjectArrayList<>(actual.values()));
                for (int index = 0; index < 12; index++) {
                    final String name = "random." + index;
                    assertEquals(expected.containsKey(name), actual.containsKey(name));
                    assertEquals(expected.get(name), actual.get(name));
                }
            }
        }
    }
}
