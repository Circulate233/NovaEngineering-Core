package github.kasuminova.novaeng.common.machine.Drills;

public class OrichalcosDrill extends Drill {
    public static final OrichalcosDrill INSTANCE = new OrichalcosDrill();

    @Override
    protected String getCoreTheardName() {
        return "novaeng.drill.thread.c";
    }

    @Override
    protected String getMachineName() {
        return "orichalcos_drill";
    }

    @Override
    protected Type getType() {
        return Type.RANGE;
    }

    @Override
    protected float getAdvancedRecipeTimeMultiple(){
        return 1.5f;
    }

    @Override
    protected long getBaseEnergy() {
        return 32768;
    }
}
