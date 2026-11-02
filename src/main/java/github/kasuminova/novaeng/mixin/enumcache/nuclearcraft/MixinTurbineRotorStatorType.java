package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.multiblock.turbine.TurbineRotorBladeUtil.TurbineRotorStatorType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TurbineRotorStatorType.class, remap = false)
public abstract class MixinTurbineRotorStatorType {

    @Shadow
    @Final
    private static TurbineRotorStatorType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TurbineRotorStatorType[] values() {
        return $VALUES;
    }
}
