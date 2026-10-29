package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.machines.machine.obelisk.weather.TileWeatherObelisk.WeatherTask;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = WeatherTask.class, remap = false)
public abstract class MixinWeatherTask {

    @Shadow
    @Final
    private static WeatherTask[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static WeatherTask[] values() {
        return $VALUES;
    }
}
