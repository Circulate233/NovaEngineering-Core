package github.kasuminova.novaeng.mixin.ar;

import github.kasuminova.novaeng.common.performance.ThreadConfinedTemperatureCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import zmaster587.advancedRocketry.api.dimension.solar.StellarBody;
import zmaster587.advancedRocketry.util.AstronomicalBodyHelper;

@Mixin(value = AstronomicalBodyHelper.class, remap = false)
public abstract class MixinAtmosphereTemperature {

    @Unique
    private static final ThreadConfinedTemperatureCache nova$temperature = new ThreadConfinedTemperatureCache();

    /**
     * @author circulation
     * @reason Reuse the numerical temperature result while all live stellar and planetary inputs remain identical.
     */
    @Overwrite
    public static int getAverageTemperature(final StellarBody star, final int orbitalDistance, final int atmPressure) {
        return nova$temperature.get(star.getTemperature(), star.getSize(), orbitalDistance, atmPressure);
    }
}
