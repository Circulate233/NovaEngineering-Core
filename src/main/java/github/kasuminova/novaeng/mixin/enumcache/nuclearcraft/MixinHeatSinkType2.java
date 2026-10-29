package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.enumm.MetaEnums.HeatSinkType2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = HeatSinkType2.class, remap = false)
public abstract class MixinHeatSinkType2 {

    @Shadow
    @Final
    private static HeatSinkType2[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static HeatSinkType2[] values() {
        return $VALUES;
    }
}
