package github.kasuminova.novaeng.client.font;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import neofontrender.internal.nightconfig.core.Config;
import neofontrender.internal.nightconfig.core.utils.StringUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundedStringListsTest {
    @Test
    void pathsKeepEmptyElementsAndReadsSeeChangedConfigValues() {
        final BoundedStringLists paths = new BoundedStringLists();
        for (final String path : List.of("", ".", "a.", ".a", "a..b", "rendering.scale")) {
            assertEquals(StringUtils.split(path, '.'), paths.path(path));
            assertSame(paths.path(path), paths.path(path));
        }
        final Config config = Config.inMemory();
        config.set("rendering.scale", 2);
        assertEquals(2, (int) config.get(paths.path("rendering.scale")));
        config.set("rendering.scale", 3);
        assertEquals(3, (int) config.get(paths.path("rendering.scale")));
        assertThrows(UnsupportedOperationException.class, () -> paths.path("a.b").clear());
    }

    @Test
    void pureWordSplittingCoversLatinCjkBidiAndControlInputs() throws Exception {
        final Class<?> type = Class.forName("neofontrender.core.font.cosmic.CosmicTextSegmenter");
        final Method method = type.getDeclaredMethod("splitReusableWords", String.class);
        method.setAccessible(true);
        final BoundedStringLists words = new BoundedStringLists();
        for (final String text : List.of("", "hello world  next", "中文测试 mixed", "abc שלום", "line\nnext", "emoji 👨‍👩‍👧 text")) {
            @SuppressWarnings("unchecked") final List<String> expected = (List<String>) method.invoke(null, text);
            words.put(text, expected);
            assertEquals(expected, words.get(text));
            assertSame(words.get(text), words.get(text));
            assertEquals(text, String.join("", words.get(text)));
        }
    }

    @Test
    void cacheBoundsAndImmutableResultsHoldUnderUniqueInputChurn() {
        final BoundedStringLists cache = new BoundedStringLists();
        final List<String> mutable = new ObjectArrayList<>(List.of("one", "two"));
        cache.put("onetwo", mutable);
        mutable.clear();
        assertEquals(List.of("one", "two"), cache.get("onetwo"));
        for (int i = 0; i < 1000; i++) cache.put("text-" + i, List.of("text-" + i));
        assertNull(cache.get("onetwo"));
        assertNotNull(cache.get("text-999"));
        cache.put("x".repeat(513), List.of("large"));
        assertNull(cache.get("x".repeat(513)));
    }
}
