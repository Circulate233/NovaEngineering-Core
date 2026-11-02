package github.kasuminova.novaeng.common.performance;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Reuses immutable character keys before Jieba consults its own canonical-character map.
 */
public final class DictionaryCharacters {

    private static final AtomicReferenceArray<AtomicReferenceArray<Character>> PAGES = new AtomicReferenceArray<>(256);

    private DictionaryCharacters() {
    }

    public static Character get(final char value) {
        if (value < 128) {
            return value;
        }
        final int pageIndex = value >>> 8;
        AtomicReferenceArray<Character> page = PAGES.get(pageIndex);
        if (page == null) {
            final AtomicReferenceArray<Character> fresh = new AtomicReferenceArray<>(256);
            final AtomicReferenceArray<Character> previous = PAGES.compareAndExchange(pageIndex, null, fresh);
            page = previous == null ? fresh : previous;
        }
        final int index = value & 255;
        final Character cached = page.get(index);
        if (cached != null) {
            return cached;
        }
        final Character fresh = value;
        final Character previous = page.compareAndExchange(index, null, fresh);
        return previous == null ? fresh : previous;
    }
}
