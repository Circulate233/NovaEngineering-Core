package github.kasuminova.novaeng.mixin.enumcache.theoneprobe;

import mcjty.theoneprobe.api.ProbeMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ProbeMode.class, remap = false)
public abstract class MixinProbeMode {

    @Shadow
    @Final
    private static ProbeMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ProbeMode[] values() {
        return $VALUES;
    }
}
