package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class OreGeometryScratch {

    public static final int AXIS_CAPACITY = 16;
    private static final ThreadLocal<ObjectArrayList<float[]>> IDLE =
        ThreadLocal.withInitial(() -> new ObjectArrayList<>(2));

    private OreGeometryScratch() {
    }

    public static float[] borrow() {
        final ObjectArrayList<float[]> idle = IDLE.get();
        return idle.isEmpty() ? new float[AXIS_CAPACITY] : idle.removeLast();
    }

    public static void release(final float[] scratch) {
        final ObjectArrayList<float[]> idle = IDLE.get();
        if (scratch.length == AXIS_CAPACITY && idle.size() < 2) {
            idle.add(scratch);
        }
    }
}
