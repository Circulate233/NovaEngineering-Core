package github.kasuminova.novaeng.mixin.ar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.util.AstronomicalBodyHelper;

@Mixin(value = DimensionProperties.class, remap = false)
public abstract class MixinDimensionProperties {

    @Shadow
    public int averageTemperature;
    @Shadow
    public double prevOrbitalTheta;
    @Shadow
    public double orbitTheta;
    @Shadow
    public int orbitalDist;
    @Shadow
    public double baseOrbitTheta;
    @Shadow
    public boolean isRetrograde;
    @Shadow
    public float[] fogColor;

    @Shadow
    public abstract StellarBody getStar();

    @Shadow
    public abstract int getSolarOrbitalDistance();

    @Shadow
    public abstract int getAtmosphereDensity();

    @Shadow
    public abstract boolean isMoon();

    @Shadow
    public abstract DimensionProperties getParentProperties();

    /**
     * @author c
     * @reason fix
     */
    @Overwrite
    public int getAverageTemp() {
        var star = this.getStar();
        if (star != null)
            this.averageTemperature = AstronomicalBodyHelper.getAverageTemperature(star, this.getSolarOrbitalDistance(), this.getAtmosphereDensity());
        else
            this.averageTemperature = 100;
        return this.averageTemperature;
    }

    /**
     * @author c
     * @reason fix
     */
    @Overwrite
    public void updateOrbit() {
        this.prevOrbitalTheta = this.orbitTheta;
        if (this.isMoon()) {
            this.orbitTheta = (AstronomicalBodyHelper.getMoonOrbitalTheta(this.orbitalDist, this.getParentProperties().gravitationalMultiplier) + this.baseOrbitTheta) * (double) (this.isRetrograde ? -1 : 1);
        } else if (!this.isMoon()) {
            var star = this.getStar();
            this.orbitTheta = (AstronomicalBodyHelper.getOrbitalTheta(this.orbitalDist, star != null ? this.getStar().getSize() : 1.0f) + this.baseOrbitTheta) * (double) (this.isRetrograde ? -1 : 1);
        }

    }

    /**
     * @author c
     * @reason fix
     */
    @Overwrite
    public float[] getSunColor() {
        var star = this.getStar();
        return star == null ? star.getColor() : new float[]{1.0F, 1.0F, 1.0F};
    }
}
