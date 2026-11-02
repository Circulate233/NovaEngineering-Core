package github.kasuminova.novaeng.common.performance;

/**
 * Numerical scratch confined to the calling thread; it never retains a star or world reference.
 */
public final class AtmosphereTemperatureCache {

    private static final double ALBEDO = Math.pow(0.7F, 0.25D);
    private boolean valid;
    private int temperature;
    private int sizeBits;
    private int distance;
    private int pressure;
    private double geometry;
    private double greenhouse;
    private int result;

    public static int calculate(final int starTemperature, final float starSize, final int orbitalDistance,
                                final int atmospherePressure) {
        final int surfaceTemperature = 58 * starTemperature;
        final float radius = starSize / 215.0F;
        final float orbit = (float) orbitalDistance / 100.0F;
        final double withoutAtmosphere = (double) surfaceTemperature * Math.pow(radius / (2.0F * orbit), 0.5D)
            * Math.pow(0.7F, 0.25D);
        return (int) (withoutAtmosphere * Math.max(1.0D, 1.125D * Math.pow((double) atmospherePressure / 100.0D, 0.25D)));
    }

    public int get(final int starTemperature, final float starSize, final int orbitalDistance,
                   final int atmospherePressure) {
        final int bits = Float.floatToRawIntBits(starSize);
        final boolean geometryChanged = !this.valid || this.sizeBits != bits || this.distance != orbitalDistance;
        final boolean pressureChanged = !this.valid || this.pressure != atmospherePressure;
        if (!geometryChanged && !pressureChanged && this.temperature == starTemperature) {
            return this.result;
        }
        if (geometryChanged) {
            final float radius = starSize / 215.0F;
            final float orbit = (float) orbitalDistance / 100.0F;
            this.geometry = Math.pow(radius / (2.0F * orbit), 0.5D);
            this.sizeBits = bits;
            this.distance = orbitalDistance;
        }
        if (pressureChanged) {
            this.greenhouse = Math.max(1.0D, 1.125D * Math.pow((double) atmospherePressure / 100.0D, 0.25D));
            this.pressure = atmospherePressure;
        }
        // Keep the original multiplication order and integer overflow before converting to double.
        final double withoutAtmosphere = (double) (58 * starTemperature) * this.geometry * ALBEDO;
        this.result = (int) (withoutAtmosphere * this.greenhouse);
        this.temperature = starTemperature;
        this.valid = true;
        return this.result;
    }
}
