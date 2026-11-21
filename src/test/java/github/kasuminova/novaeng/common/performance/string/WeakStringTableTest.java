package github.kasuminova.novaeng.common.performance.string;

import com.sun.management.ThreadMXBean;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeakStringTableTest {
    private static void hit(final WeakStringTable table, final String expected) {
        if (table.slice("/namespace:some/path/", 1, 20) != expected
            || table.lower("NAMESPACE:SOME/PATH", Locale.ROOT) != expected
            || table.join("namespace", ':', "some/path") != expected
            || table.canonicalize(expected) != expected) throw new AssertionError();
    }

    @Test
    void slicesLowercaseAndConcatenationsShareExactObjects() {
        final WeakStringTable table = new WeakStringTable(128, 128);
        final String path = table.canonicalize("test:path");
        assertSame(path, table.slice("__test:path__", 2, 11));
        assertSame(path, table.lower("TEST:PATH", Locale.ROOT));
        assertSame(path, table.join("test", ':', "path"));
        assertNotSame(table.canonicalize("Aa"), table.canonicalize("BB"));
        assertEquals("Aa", table.slice("/Aa/", 1, 3));
        assertEquals("BB", table.slice("/BB/", 1, 3));
        assertNull(table.canonicalize(null));
        assertSame("", table.slice("abc", 1, 1));
        assertThrows(StringIndexOutOfBoundsException.class, () -> table.slice("x", 1, 0));
    }

    @Test
    void fullUnicodeAndNonRootLocaleRulesArePreserved() {
        final WeakStringTable table = new WeakStringTable(128, 128);
        for (final String value : List.of("İ", "ΟΣ", "ΣΣ", "AΣ", "\uD801\uDC00", "Straße", "ASCII_42")) {
            for (final Locale locale : List.of(Locale.ROOT, Locale.forLanguageTag("tr"), Locale.forLanguageTag("lt"))) {
                assertEquals(value.toLowerCase(locale), table.lower(value, locale), value + " / " + locale);
            }
        }
    }

    @Test
    void onlyFixedWeakSlotsSurviveUniqueInputsAndDeadEntriesCanBeReplaced() throws Exception {
        final WeakStringTable table = new WeakStringTable(128, 128);
        final List<String> live = new ArrayList<>();
        for (int i = 0; i < 4096; i++) live.add(table.canonicalize("unique-" + i));
        final var field = WeakStringTable.class.getDeclaredField("entries");
        field.setAccessible(true);
        final WeakReference<?>[] entries = (WeakReference<?>[]) field.get(table);
        assertEquals(128, entries.length);
        for (final WeakReference<?> entry : entries) if (entry != null) entry.clear();
        final String revived = table.canonicalize("unique-4095");
        assertSame(revived, table.slice("/unique-4095/", 1, 12));
        assertEquals(4096, live.size());
        final String oversized = "a".repeat(129);
        assertSame(oversized, table.canonicalize(oversized));
    }

    @Test
    void concurrentHitsPublishOneLiveValue() throws Exception {
        final WeakStringTable table = new WeakStringTable(128, 128);
        try (final var executor = Executors.newFixedThreadPool(8)) {
            final var jobs = new ArrayList<Callable<String>>();
            for (int i = 0; i < 128; i++) jobs.add(() -> table.slice("/threaded-value/", 1, 15));
            final var results = executor.invokeAll(jobs);
            final String expected = results.getFirst().get();
            for (final var result : results) assertSame(expected, result.get());
        }
    }

    @Test
    void repeatedHitsAllocateNoSubstringLowercaseOrJoinStorage() {
        final WeakStringTable table = new WeakStringTable(128, 128);
        final String expected = table.canonicalize("namespace:some/path");
        final ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        for (int i = 0; i < 50000; i++) hit(table, expected);
        final long thread = Thread.currentThread().threadId();
        final long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 100000; i++) hit(table, expected);
        final long allocated = bean.getThreadAllocatedBytes(thread) - before;
        assertTrue(allocated < 8192, "400,000 warmed hits allocated " + allocated + " bytes");
    }
}
