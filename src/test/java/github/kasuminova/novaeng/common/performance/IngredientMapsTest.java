package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class IngredientMapsTest {
    static void main(String[] args) {
        new IngredientMapsTest().preservesIdentityMapOrderAtEveryInsertionAndReplacement();
        System.out.println("PASS: 50,000 identity-map insertion/replacement states, including null and growth");
    }

    @Test
    void preservesIdentityMapOrderAtEveryInsertionAndReplacement() {
        Random random = new Random(20261126);
        for (int trial = 0; trial < 200; trial++) {
            var baseline = new Reference2ObjectOpenHashMap<Object, List<List<String>>>();
            var candidate = new Reference2ObjectOpenHashMap<Object, List<List<String>>>(1);
            List<Object> keys = new ArrayList<>();
            keys.add(null);
            for (int i = 0; i < 80; i++) keys.add(new EqualKey());
            // Include null first, null second, repeated singleton keys, and growth past
            // the default map's rehash thresholds. Equal keys have distinct identities.
            for (int insertion = 0; insertion < 250; insertion++) {
                Object key = keys.get(random.nextInt(Math.min(keys.size(), insertion / 3 + 1)));
                List<List<String>> values = new ArrayList<>(List.of(new ArrayList<>(Arrays.asList("a", null, "a"))));
                assertSame(baseline.put(key, values), IngredientMaps.put(candidate, key, values));
                List<Object> expectedOrder = new ArrayList<>(baseline.keySet());
                List<Object> actualOrder = new ArrayList<>(candidate.keySet());
                for (int i = 0; i < expectedOrder.size(); i++) assertSame(expectedOrder.get(i), actualOrder.get(i));
                assertEquals(baseline.size(), candidate.size());
                for (Object type : baseline.keySet()) assertSame(baseline.get(type), candidate.get(type));
            }
            // No group copies, deduplication, wrapping or immutability are introduced.
            candidate.get(null).get(0).add("mutated");
            assertSame(baseline.get(null), candidate.get(null));
        }
    }

    private static final class EqualKey {
        @Override
        public boolean equals(Object other) {
            return other instanceof EqualKey;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
