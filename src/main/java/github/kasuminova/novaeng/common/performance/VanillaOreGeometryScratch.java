package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class VanillaOreGeometryScratch {

    public static final int AXIS_CAPACITY = 16;
    private static final ThreadLocal<ObjectArrayList<double[]>> IDLE =
        ThreadLocal.withInitial(() -> new ObjectArrayList<>(2));

    private VanillaOreGeometryScratch() {
    }

    public static double[] borrow() {
        final ObjectArrayList<double[]> idle = IDLE.get();
        return idle.isEmpty() ? new double[AXIS_CAPACITY] : idle.removeLast();
    }

    public static void release(final double[] scratch) {
        final ObjectArrayList<double[]> idle = IDLE.get();
        if (scratch.length == AXIS_CAPACITY && idle.size() < 2) {
            idle.add(scratch);
        }
    }
}
