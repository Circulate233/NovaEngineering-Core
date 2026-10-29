package github.kasuminova.novaeng.mixin.enumcache.advancedrocketry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.dimension.DimensionProperties.AtmosphereTypes;

@Mixin(value = AtmosphereTypes.class, remap = false)
public abstract class MixinAtmosphereTypes {

    @Shadow
    @Final
    private static AtmosphereTypes[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AtmosphereTypes[] values() {
        return $VALUES;
    }
}
