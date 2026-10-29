package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.base.IRedstoneControl.RedstoneControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RedstoneControl.class, remap = false)
public abstract class MixinRedstoneControl {

    @Shadow
    @Final
    private static RedstoneControl[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RedstoneControl[] values() {
        return $VALUES;
    }
}
