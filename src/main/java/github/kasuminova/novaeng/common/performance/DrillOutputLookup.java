package github.kasuminova.novaeng.common.performance;

public final class DrillOutputLookup {

    private static final double[] RESEARCH_DAMAGE = new double[256];
    private static final double[] COMPONENT_MULTIPLIER = new double[256];
    private static final String[] PARALLEL_KEYS = new String[9];
    private static final String[] DEPLETION_KEYS = new String[9];
    private static final String[] VEIN_KEYS = new String[9];
    private static final String[] CRYSTALS = {
        "environmentaltech:litherite_crystal", "environmentaltech:erodium_crystal",
        "environmentaltech:kyronite_crystal", "environmentaltech:pladium_crystal",
        "environmentaltech:ionite_crystal", "environmentaltech:aethium_crystal"
    };

    static {
        for (int i = 0; i < 256; i++) {
            final byte value = (byte) i;
            RESEARCH_DAMAGE[i] = 1000 * Math.pow(value, 1.6);
            COMPONENT_MULTIPLIER[i] = 1 + Math.pow(value, 2.5);
        }
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                final int index = x * 3 + z;
                final String suffix = Integer.toString(x) + z;
                PARALLEL_KEYS[index] = "parallelSize" + suffix;
                DEPLETION_KEYS[index] = "depletion" + suffix;
                VEIN_KEYS[index] = "veinName" + suffix;
            }
        }
    }

    private DrillOutputLookup() {
    }

    public static double researchDamage(final byte research) {
        return RESEARCH_DAMAGE[research & 255];
    }

    public static double componentMultiplier(final byte components) {
        return COMPONENT_MULTIPLIER[components & 255];
    }

    public static String parallelKey(final int x, final int z) {
        return PARALLEL_KEYS[x * 3 + z];
    }

    public static String depletionKey(final int x, final int z) {
        return DEPLETION_KEYS[x * 3 + z];
    }

    public static String veinKey(final int x, final int z) {
        return VEIN_KEYS[x * 3 + z];
    }

    public static int crystalCount(final int research) {
        return research <= 1 ? 2 : research == 2 ? 4 : research == 3 ? 5 : 6;
    }

    public static String crystalName(final int index) {
        return CRYSTALS[index];
    }
}
