package github.kasuminova.novaeng.mixin.enumcache.advancedrocketry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.api.fuel.FuelRegistry.FuelType;

@Mixin(value = FuelType.class, remap = false)
public abstract class MixinFuelType {

    @Shadow
    @Final
    private static FuelType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FuelType[] values() {
        return $VALUES;
    }
}
