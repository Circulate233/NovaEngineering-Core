package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.multiblock.turbine.TurbineRotorBladeUtil.TurbineRotorBladeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TurbineRotorBladeType.class, remap = false)
public abstract class MixinTurbineRotorBladeType {

    @Shadow
    @Final
    private static TurbineRotorBladeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TurbineRotorBladeType[] values() {
        return $VALUES;
    }
}
