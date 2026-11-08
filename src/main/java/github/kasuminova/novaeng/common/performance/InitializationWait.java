package github.kasuminova.novaeng.common.performance;

import java.util.function.BooleanSupplier;

public final class InitializationWait {
    private InitializationWait() {
    }

    /** Like the original sleep, an interrupt ends this interval; spurious notifications do not shorten it. */
    public static void await(final Object monitor, final BooleanSupplier ready, final long millis) {
        final long deadline = System.nanoTime() + millis * 1_000_000L;
        synchronized (monitor) {
            while (!ready.getAsBoolean()) {
                final long remaining = deadline - System.nanoTime();
                if (remaining <= 0) return;
                try {
                    monitor.wait(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
                } catch (InterruptedException interrupted) {
                    return;
                }
            }
        }
    }
}
