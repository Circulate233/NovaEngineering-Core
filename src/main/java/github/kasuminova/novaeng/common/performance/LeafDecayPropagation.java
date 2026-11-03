package github.kasuminova.novaeng.common.performance;

/**
 * Propagates four leaf-distance layers without rescanning the whole cube for every layer.
 */
public final class LeafDecayPropagation {

    private static final ThreadLocal<int[]> QUEUE = ThreadLocal.withInitial(() -> new int[729]);

    private LeafDecayPropagation() {
    }

    public static void propagate(final int[] surroundings) {
        int supports = 0;
        for (int x = 12; x <= 20; x++) {
            for (int y = 12; y <= 20; y++) {
                for (int z = 12; z <= 20; z++) {
                    if (surroundings[x * 1024 + y * 32 + z] == 0) supports++;
                }
            }
        }
        propagate(surroundings, supports);
    }

    public static void propagate(final int[] surroundings, final int supports) {
        propagate(surroundings, supports, -1);
    }

    public static void propagate(final int[] surroundings, final int supports, final int onlySupport) {
        if (supports == 0) return;
        if (supports > 16) {
            scan(surroundings);
            return;
        }
        final int[] queue = QUEUE.get();
        int tail = 0;
        if (supports == 1 && onlySupport >= 0) {
            queue[tail++] = onlySupport;
        } else {
            for (int x = 12; x <= 20; x++) {
                for (int y = 12; y <= 20; y++) {
                    final int base = x * 1024 + y * 32;
                    for (int z = 12; z <= 20; z++) {
                        final int index = base + z;
                        if (surroundings[index] == 0) {
                            queue[tail++] = index;
                        }
                    }
                }
            }
        }
        int head = 0;
        for (int distance = 1; distance <= 4 && head < tail; distance++) {
            final int end = tail;
            while (head < end) {
                final int index = queue[head++];
                if (distance == 4) {
                    tail = visit(surroundings, queue, tail, index - 1024, distance, false);
                    tail = visit(surroundings, queue, tail, index + 1024, distance, false);
                    tail = visit(surroundings, queue, tail, index - 32, distance, false);
                    tail = visit(surroundings, queue, tail, index + 32, distance, false);
                    tail = visit(surroundings, queue, tail, index - 1, distance, false);
                    tail = visit(surroundings, queue, tail, index + 1, distance, false);
                } else {
                    final int x = index >>> 10;
                    final int y = index >>> 5 & 31;
                    final int z = index & 31;
                    tail = visit(surroundings, queue, tail, index - 1024, distance, x > 12);
                    tail = visit(surroundings, queue, tail, index + 1024, distance, x < 20);
                    tail = visit(surroundings, queue, tail, index - 32, distance, y > 12);
                    tail = visit(surroundings, queue, tail, index + 32, distance, y < 20);
                    tail = visit(surroundings, queue, tail, index - 1, distance, z > 12);
                    tail = visit(surroundings, queue, tail, index + 1, distance, z < 20);
                }
            }
        }
    }

    private static void scan(final int[] surroundings) {
        for (int distance = 1; distance <= 4; distance++) {
            for (int x = 12; x <= 20; x++) {
                for (int y = 12; y <= 20; y++) {
                    for (int z = 12; z <= 20; z++) {
                        final int index = x * 1024 + y * 32 + z;
                        if (surroundings[index] == distance - 1) {
                            if (surroundings[index - 1024] == -2) surroundings[index - 1024] = distance;
                            if (surroundings[index + 1024] == -2) surroundings[index + 1024] = distance;
                            if (surroundings[index - 32] == -2) surroundings[index - 32] = distance;
                            if (surroundings[index + 32] == -2) surroundings[index + 32] = distance;
                            if (surroundings[index - 1] == -2) surroundings[index - 1] = distance;
                            if (surroundings[index + 1] == -2) surroundings[index + 1] = distance;
                        }
                    }
                }
            }
        }
    }

    private static int visit(final int[] surroundings, final int[] queue, final int tail,
                             final int index, final int distance, final boolean enqueue) {
        if (surroundings[index] != -2) {
            return tail;
        }
        surroundings[index] = distance;
        if (enqueue) {
            queue[tail] = index;
            return tail + 1;
        }
        return tail;
    }
}
