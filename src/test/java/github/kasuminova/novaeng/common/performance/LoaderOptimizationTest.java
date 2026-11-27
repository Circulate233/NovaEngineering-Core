package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.util.DeclaredMethodTable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LoaderOptimizationTest {
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
        final List<Thread> workers = new ObjectArrayList<>();
        final List<Throwable> failures = java.util.Collections.synchronizedList(new ObjectArrayList<>());
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
        for (int index = 0; index < workers.size(); index++) workers.get(index).join(10_000);
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
