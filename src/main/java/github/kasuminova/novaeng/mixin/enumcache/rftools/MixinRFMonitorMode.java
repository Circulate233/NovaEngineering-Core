package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.blocks.monitor.RFMonitorMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RFMonitorMode.class, remap = false)
public abstract class MixinRFMonitorMode {

    @Shadow
    @Final
    private static RFMonitorMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RFMonitorMode[] values() {
        return $VALUES;
    }
}
