package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.api.farm.FarmNotification;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FarmNotification.class, remap = false)
public abstract class MixinFarmNotification {

    @Shadow
    @Final
    private static FarmNotification[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FarmNotification[] values() {
        return $VALUES;
    }
}
