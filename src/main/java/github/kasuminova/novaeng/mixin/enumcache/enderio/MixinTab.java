package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.powertools.machine.monitor.GuiPowerMonitor.Tab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Tab.class, remap = false)
public abstract class MixinTab {

    @Shadow
    @Final
    private static Tab[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Tab[] values() {
        return $VALUES;
    }
}
