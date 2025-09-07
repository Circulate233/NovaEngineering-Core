package github.kasuminova.novaeng.common.machine.Drills;

public class MineralExtractor extends Drill{
    public static final MineralExtractor INSTANCE = new MineralExtractor();

    @Override
    protected String getCoreTheardName() {
        return "novaeng.drill.thread.a";
    }
    @Override
    protected String getMachineName() {
        return "mineral_extractor";
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
