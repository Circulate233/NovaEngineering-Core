package github.kasuminova.novaeng.common.machine.drills;

public class ManaOreDrill extends Drill {
    public static final ManaOreDrill INSTANCE = new ManaOreDrill();

    @Override
    protected String getCoreTheardName() {
        return "novaeng.drill.thread.c";
    }

    @Override
    protected String getMachineName() {
        return "mana_ore_drill";
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
