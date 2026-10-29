package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.tile.internal.fluid.TankOutputSetting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TankOutputSetting.class, remap = false)
public abstract class MixinTankOutputSetting {

    @Shadow
    @Final
    private static TankOutputSetting[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TankOutputSetting[] values() {
        return $VALUES;
    }
}
