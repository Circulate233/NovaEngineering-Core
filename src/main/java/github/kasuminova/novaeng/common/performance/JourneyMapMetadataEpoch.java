package github.kasuminova.novaeng.common.performance;

import java.util.concurrent.atomic.AtomicLong;

/** Does not initialize the client writer/cache during early registry construction or on a server. */
public final class JourneyMapMetadataEpoch {
    private static final AtomicLong EPOCH = new AtomicLong();

    private JourneyMapMetadataEpoch() {
    }

    public static long current() {
        return EPOCH.get();
    }

    public static void invalidate() {
        EPOCH.incrementAndGet();
    }
}
