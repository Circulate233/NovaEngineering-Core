package github.kasuminova.novaeng.common.machine.drills;

public final class SmallOreDrill extends Drill {

    public static final SmallOreDrill INSTANCE = new SmallOreDrill();

    private SmallOreDrill() {
    }

    @Override
    public String getCoreTheardName() {
        return "novaeng.drill.thread.a";
    }

    @Override
    public String getMachineName() {
        return "small_ore_drill";
    }

    @Override
    public Type getType() {
        return Type.SINGLE;
    }

    @Override
    public long getBaseEnergy() {
        return 24576;
    }
}
