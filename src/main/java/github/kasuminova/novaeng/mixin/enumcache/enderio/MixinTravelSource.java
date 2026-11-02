package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.api.teleport.TravelSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TravelSource.class, remap = false)
public abstract class MixinTravelSource {

    @Shadow
    @Final
    private static TravelSource[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TravelSource[] values() {
        return $VALUES;
    }
}
