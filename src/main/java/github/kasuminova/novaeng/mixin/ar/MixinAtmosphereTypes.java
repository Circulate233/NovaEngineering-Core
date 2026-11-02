package github.kasuminova.novaeng.mixin.ar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zmaster587.advancedRocketry.dimension.DimensionProperties;

@Mixin(value = DimensionProperties.AtmosphereTypes.class, remap = false)
public abstract class MixinAtmosphereTypes {

    @Unique
    private static final DimensionProperties.AtmosphereTypes[] nova$types = DimensionProperties.AtmosphereTypes.values();

    @Redirect(method = "getAtmosphereTypeFromValue", at = @At(value = "INVOKE",
        target = "Lzmaster587/advancedRocketry/dimension/DimensionProperties$AtmosphereTypes;values()[Lzmaster587/advancedRocketry/dimension/DimensionProperties$AtmosphereTypes;"), require = 1)
    private static DimensionProperties.AtmosphereTypes[] nova$classificationValues() {
        return nova$types;
    }
}
