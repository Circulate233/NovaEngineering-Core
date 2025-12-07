package github.kasuminova.novaeng.common.machine.drills;

public class VoidMiner extends Drill {
    public static final VoidMiner INSTANCE = new VoidMiner();

    @Override
    protected String getCoreTheardName() {
        return "novaeng.drill.thread.b";
    }

    @Override
    protected String getMachineName() {
        return "void_miner";
    }

    @Override
    protected Type getType() {
        return Type.SINGLE;
    }

    @Override
    protected long getBaseEnergy() {
        return 24576;
    }

    @Override
    protected float getRecipeTimeMultiple() {
        return 1.5f;
    }
}
