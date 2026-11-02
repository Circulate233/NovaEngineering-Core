package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.multiblock.turbine.TurbineDynamoCoilType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TurbineDynamoCoilType.class, remap = false)
public abstract class MixinTurbineDynamoCoilType {

    @Shadow
    @Final
    private static TurbineDynamoCoilType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TurbineDynamoCoilType[] values() {
        return $VALUES;
    }
}
