package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.world.BOPWorldSettings.RainfallVariationScheme;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RainfallVariationScheme.class, remap = false)
public abstract class MixinRainfallVariationScheme {

    @Shadow
    @Final
    private static RainfallVariationScheme[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RainfallVariationScheme[] values() {
        return $VALUES;
    }
}
