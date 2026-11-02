package github.kasuminova.novaeng.common.machine.drills;

public final class DifferentWorld extends Drill {

    public static final DifferentWorld INSTANCE = new DifferentWorld();

    private DifferentWorld() {
    }

    @Override
    public String getCoreTheardName() {
        return "novaeng.drill.thread.b";
    }

    @Override
    public String getMachineName() {
        return "different_world";
    }

    @Override
    public Type getType() {
        return Type.RANGE;
    }

    @Override
    public float getRecipeTimeMultiple() {
        return 1.5f;
    }

    @Override
    public float getAdvancedRecipeTimeMultiple() {
        return 1.5f;
    }

    @Override
    public long getBaseEnergy() {
        return 32768;
    }

    @Override
    public boolean isDimensional() {
        return true;
    }
}
