package github.kasuminova.novaeng.common.machine.drills;

public final class MineralExtractor extends Drill {

    public static final MineralExtractor INSTANCE = new MineralExtractor();

    private MineralExtractor() {
    }

    @Override
    public String getCoreTheardName() {
        return "novaeng.drill.thread.a";
    }

    @Override
    public String getMachineName() {
        return "mineral_extractor";
    }

    @Override
    public Type getType() {
        return Type.RANGE;
    }

    @Override
    public float getAdvancedRecipeTimeMultiple() {
        return 1.5f;
    }

    @Override
    public long getBaseEnergy() {
        return 32768;
    }
}
