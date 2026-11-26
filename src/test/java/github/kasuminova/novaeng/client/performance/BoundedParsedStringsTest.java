package github.kasuminova.novaeng.client.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundedParsedStringsTest {
    @Test
    void inputChangesAndHashCollisionsCannotReturnAnOldConfigValue() {
        final BoundedParsedStrings<Integer> cache = new BoundedParsedStrings<>(1);
        cache.put("red", 0xFF0000);
        assertEquals(0xFF0000, cache.get(new String("red")).value());
        assertNull(cache.get("blue"));
        cache.put("blue", 0x0000FF);
        assertNull(cache.get("red"));
        assertEquals(0x0000FF, cache.get("blue").value());
        cache.put("invalid", null);
        assertNotNull(cache.get("invalid"));
        assertNull(cache.get("invalid").value());
    }

    @Test
    void retainedKeysAreBoundedAndOversizedInputsAreNotRetained() {
        final BoundedParsedStrings<String> cache = new BoundedParsedStrings<>(4);
        for (int i = 0; i < 1000; i++) cache.put("key" + i, "value");
        int hits = 0;
        for (int i = 0; i < 1000; i++) if (cache.get("key" + i) != null) hits++;
        assertEquals(4, hits);
        cache.put("x".repeat(4097), "large");
        assertNull(cache.get("x".repeat(4097)));
        cache.put(null, "null");
        assertNull(cache.get(null));
    }
}
