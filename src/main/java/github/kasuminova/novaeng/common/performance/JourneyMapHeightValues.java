package github.kasuminova.novaeng.common.performance;

import java.util.concurrent.atomic.AtomicReferenceArray;

/** A bounded, lazy cache for JourneyMap's boxed scan heights, including extended/negative worlds. */
public final class JourneyMapHeightValues {
    private static final int MIN = -4096;
    private static final int MAX = 4095;
    private static final AtomicReferenceArray<Integer> VALUES = new AtomicReferenceArray<>(MAX - MIN + 1);

    private JourneyMapHeightValues() {
    }

    public static Integer valueOf(final int value) {
        if (value >= -128 && value <= 127 || value < MIN || value > MAX) return Integer.valueOf(value);
        final int index = value - MIN;
        Integer cached = VALUES.get(index);
        if (cached != null) return cached;
        cached = Integer.valueOf(value);
        if (VALUES.compareAndSet(index, null, cached)) return cached;
        return VALUES.get(index);
    }
}
