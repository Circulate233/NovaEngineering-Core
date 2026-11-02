package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TemperatureUnit.class, remap = false)
public abstract class MixinTemperatureUnit {

    @Shadow
    @Final
    private static TemperatureUnit[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TemperatureUnit[] values() {
        return $VALUES;
    }
}
