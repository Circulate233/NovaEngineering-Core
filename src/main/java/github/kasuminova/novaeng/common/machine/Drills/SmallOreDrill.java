package github.kasuminova.novaeng.common.machine.Drills;

public class SmallOreDrill extends Drill {
    public static final SmallOreDrill INSTANCE = new SmallOreDrill();

    @Override
    protected String getCoreTheardName() {
        return "novaeng.drill.thread.a";
    }

    @Override
    protected String getMachineName() {
        return "small_ore_drill";
    }

    @Override
    protected Type getType() {
        return Type.SINGLE;
    }

    @Override
    protected long getBaseEnergy() {
        return 24576;
    }
}
