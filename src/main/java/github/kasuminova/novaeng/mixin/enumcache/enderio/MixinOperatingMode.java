package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.machines.machine.alloy.OperatingMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = OperatingMode.class, remap = false)
public abstract class MixinOperatingMode {

    @Shadow
    @Final
    private static OperatingMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static OperatingMode[] values() {
        return $VALUES;
    }
}
