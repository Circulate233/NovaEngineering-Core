package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.machine.modes.IoMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = IoMode.class, remap = false)
public abstract class MixinIoMode {

    @Shadow
    @Final
    private static IoMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static IoMode[] values() {
        return $VALUES;
    }
}
