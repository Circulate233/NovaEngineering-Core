package github.kasuminova.novaeng.mixin.enumcache.theoneprobe;

import mcjty.theoneprobe.api.IProbeConfig.ConfigMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConfigMode.class, remap = false)
public abstract class MixinConfigMode {

    @Shadow
    @Final
    private static ConfigMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConfigMode[] values() {
        return $VALUES;
    }
}
