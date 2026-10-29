package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.machines.machine.tank.VoidMode.IconHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = IconHolder.class, remap = false)
public abstract class MixinVoidModeIconHolder {

    @Shadow
    @Final
    private static IconHolder[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static IconHolder[] values() {
        return $VALUES;
    }
}
