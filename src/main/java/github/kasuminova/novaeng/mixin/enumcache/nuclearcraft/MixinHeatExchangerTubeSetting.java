package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.multiblock.hx.HeatExchangerTubeSetting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = HeatExchangerTubeSetting.class, remap = false)
public abstract class MixinHeatExchangerTubeSetting {

    @Shadow
    @Final
    private static HeatExchangerTubeSetting[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static HeatExchangerTubeSetting[] values() {
        return $VALUES;
    }
}
