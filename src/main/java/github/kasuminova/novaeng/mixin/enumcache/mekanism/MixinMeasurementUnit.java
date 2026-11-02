package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.util.UnitDisplayUtils.MeasurementUnit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MeasurementUnit.class, remap = false)
public abstract class MixinMeasurementUnit {

    @Shadow
    @Final
    private static MeasurementUnit[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MeasurementUnit[] values() {
        return $VALUES;
    }
}
