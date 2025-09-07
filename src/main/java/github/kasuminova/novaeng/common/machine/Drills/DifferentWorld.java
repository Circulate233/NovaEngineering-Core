package github.kasuminova.novaeng.common.machine.Drills;

public class DifferentWorld extends Drill {
    public static final DifferentWorld INSTANCE = new DifferentWorld();

    @Override
    protected String getCoreTheardName() {
        return "novaeng.drill.thread.b";
    }

    @Override
    protected String getMachineName() {
        return "different_world";
    }

    @Override
    protected Type getType() {
        return Type.RANGE;
    }

    @Override
    protected float getRecipeTimeMultiple(){
        return 1.5f;
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
