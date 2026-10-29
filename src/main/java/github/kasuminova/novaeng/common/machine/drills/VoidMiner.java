package github.kasuminova.novaeng.common.machine.drills;

public final class VoidMiner extends Drill {

    public static final VoidMiner INSTANCE = new VoidMiner();

    private VoidMiner() {
    }

    @Override
    public String getCoreTheardName() {
        return "novaeng.drill.thread.b";
    }

    @Override
    public String getMachineName() {
        return "void_miner";
    }

    @Override
    public Type getType() {
        return Type.SINGLE;
    }

    @Override
    public long getBaseEnergy() {
        return 24576;
    }

    @Override
    public float getRecipeTimeMultiple() {
        return 1.5f;
    }

    @Override
    public boolean isDimensional() {
        return true;
    }
}
