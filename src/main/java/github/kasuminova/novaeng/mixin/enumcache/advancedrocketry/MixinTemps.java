package github.kasuminova.novaeng.mixin.enumcache.advancedrocketry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.dimension.DimensionProperties.Temps;

@Mixin(value = Temps.class, remap = false)
public abstract class MixinTemps {

    @Shadow
    @Final
    private static Temps[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Temps[] values() {
        return $VALUES;
    }
}
