package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.client.HolidayManager.Month;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Month.class, remap = false)
public abstract class MixinMonth {

    @Shadow
    @Final
    private static Month[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Month[] values() {
        return $VALUES;
    }
}
