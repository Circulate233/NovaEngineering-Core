package github.kasuminova.novaeng.mixin.enumcache.armourers_workshop;

import moe.plushie.armourers_workshop.common.tileentities.TileEntityHologramProjector.PowerMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PowerMode.class, remap = false)
public abstract class MixinPowerMode {

    @Shadow
    @Final
    private static PowerMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PowerMode[] values() {
        return $VALUES;
    }
}
