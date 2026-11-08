package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.util.DeclaredMethodTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LoaderOptimizationTest {
    @Test
    void languageReaderMatchesLineParserAndDoesNotCloseInput() throws Exception {
        final Random random = new Random(1906);
        final String[] samples = {"", "#comment", "missing", "=empty key", "a=", "a=b=c", " space = kept ",
            "unicode=你好🙂", "format=%1$05.2f/%d/%s", "bad=%1$..f", "\ufeffbom=value"};
        final String[] endings = {"\n", "\r", "\r\n"};
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < 10_000; i++) text.append(samples[random.nextInt(samples.length)]).append(endings[random.nextInt(3)]);
        text.append("tail=without newline");
        final byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
        final Map<String, String> expected = new HashMap<>();
        for (final String line : org.apache.commons.io.IOUtils.readLines(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            LanguageLineParser.put(expected, line);
        }
        final Map<String, String> actual = new HashMap<>();
        final InputStream stream = new ByteArrayInputStream(bytes) {
            @Override public void close() { fail("Caller owns the input"); }
            @Override public synchronized int read(byte[] target, int offset, int length) {
                return super.read(target, offset, Math.min(13, length));
            }
        };
        LanguageLineParser.read(actual, stream);
        assertEquals(expected, actual);
    }

    @Test
    void failedLanguageReadDoesNotPartiallyPublish() {
        final Map<String, String> properties = new HashMap<>(Map.of("existing", "value"));
        final byte[] prefix = "new=value\n".getBytes(StandardCharsets.UTF_8);
        final InputStream stream = new InputStream() {
            int index;
            @Override public int read() throws IOException {
                if (index < prefix.length) return prefix[index++];
                throw new IOException("read failure");
            }
        };
        assertThrows(IOException.class, () -> LanguageLineParser.read(properties, stream));
        assertEquals(Map.of("existing", "value"), properties);
    }

    interface Parent { Number value(); }
    static class Methods implements Parent {
        @Override public Integer value() { return 1; }
        public void overload(int value) { }
        public void overload(String value) { }
        private void hidden(String value, long number) { }
        public void Aa(int value) { }
        public void BB(int value) { } // Deliberately collides with Aa's String hash.
    }
    static class Child extends Methods { }

    @Test
    void methodIndexMatchesReflectionIncludingBridgesCollisionsAndConcurrency() throws Exception {
        final List<Thread> workers = new ArrayList<>();
        final List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 8; i++) {
            final Thread worker = new Thread(() -> {
                try {
                    for (int repeat = 0; repeat < 200; repeat++) {
                        for (final Method method : Methods.class.getDeclaredMethods()) {
                            assertEquals(Methods.class.getDeclaredMethod(method.getName(), method.getParameterTypes()),
                                DeclaredMethodTable.lookup(Methods.class, method.getName(), method.getParameterTypes()));
                        }
                        assertNull(DeclaredMethodTable.lookup(Methods.class, "missing", new Class<?>[0]));
                        assertNull(DeclaredMethodTable.lookup(Child.class, "value", new Class<?>[0]));
                    }
                } catch (Throwable failure) { failures.add(failure); }
            });
            workers.add(worker);
            worker.start();
        }
        for (final Thread worker : workers) worker.join(10_000);
        assertTrue(workers.stream().noneMatch(Thread::isAlive));
        assertTrue(failures.isEmpty(), failures.toString());
        assertArrayEquals(Child.class.getMethods(), DeclaredMethodTable.publicMethods(Child.class));
    }

    @Test
    void initializationWaitHandlesReadyNotificationSpuriousWakeupsAndInterrupt() throws Exception {
        final Object monitor = new Object();
        final boolean[] ready = {false};
        final CountDownLatch checked = new CountDownLatch(1);
        final Thread waiter = new Thread(() -> InitializationWait.await(monitor, () -> {
            checked.countDown();
            return ready[0];
        }, 5_000));
        waiter.start();
        assertTrue(checked.await(2, TimeUnit.SECONDS));
        synchronized (monitor) { monitor.notifyAll(); }
        waiter.join(30);
        assertTrue(waiter.isAlive(), "A spurious wakeup must not finish the interval");
        synchronized (monitor) { ready[0] = true; monitor.notifyAll(); }
        waiter.join(2_000);
        assertFalse(waiter.isAlive());
        InitializationWait.await(monitor, () -> true, 5_000);
        final Thread interrupted = new Thread(() -> InitializationWait.await(monitor, () -> false, 5_000));
        interrupted.start();
        interrupted.interrupt();
        interrupted.join(2_000);
        assertFalse(interrupted.isAlive());
        InitializationWait.await(monitor, () -> false, 1);
    }
}
