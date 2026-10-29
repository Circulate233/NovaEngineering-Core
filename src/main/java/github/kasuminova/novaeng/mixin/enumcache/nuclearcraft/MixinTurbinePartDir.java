package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.multiblock.turbine.TurbineRotorBladeUtil.TurbinePartDir;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TurbinePartDir.class, remap = false)
public abstract class MixinTurbinePartDir {

    @Shadow
    @Final
    private static TurbinePartDir[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TurbinePartDir[] values() {
        return $VALUES;
    }
}
