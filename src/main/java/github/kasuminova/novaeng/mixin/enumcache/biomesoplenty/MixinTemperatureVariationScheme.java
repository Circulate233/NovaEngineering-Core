package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.world.BOPWorldSettings.TemperatureVariationScheme;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TemperatureVariationScheme.class, remap = false)
public abstract class MixinTemperatureVariationScheme {

    @Shadow
    @Final
    private static TemperatureVariationScheme[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TemperatureVariationScheme[] values() {
        return $VALUES;
    }
}
