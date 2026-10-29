package github.kasuminova.novaeng.mixin.enumcache.theoneprobe;

import mcjty.theoneprobe.api.NumberFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = NumberFormat.class, remap = false)
public abstract class MixinNumberFormat {

    @Shadow
    @Final
    private static NumberFormat[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static NumberFormat[] values() {
        return $VALUES;
    }
}
