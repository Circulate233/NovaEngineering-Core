package github.kasuminova.novaeng.mixin.ar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import zmaster587.advancedRocketry.dimension.DimensionProperties;

@Mixin(value = DimensionProperties.Temps.class, remap = false)
public abstract class MixinAtmosphereTemps {

    @Unique
    private static final DimensionProperties.Temps[] nova$temps = DimensionProperties.Temps.values();

    @Redirect(method = "getTempFromValue", at = @At(value = "INVOKE",
        target = "Lzmaster587/advancedRocketry/dimension/DimensionProperties$Temps;values()[Lzmaster587/advancedRocketry/dimension/DimensionProperties$Temps;"), require = 1)
    private static DimensionProperties.Temps[] nova$classificationValues() {
        return nova$temps;
    }
}
