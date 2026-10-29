package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.multiblock.hx.HeatExchangerTubeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = HeatExchangerTubeType.class, remap = false)
public abstract class MixinHeatExchangerTubeType {

    @Shadow
    @Final
    private static HeatExchangerTubeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static HeatExchangerTubeType[] values() {
        return $VALUES;
    }
}
