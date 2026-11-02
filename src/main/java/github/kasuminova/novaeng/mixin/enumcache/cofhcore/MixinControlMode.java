package github.kasuminova.novaeng.mixin.enumcache.cofhcore;

import cofh.api.tileentity.IRedstoneControl.ControlMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ControlMode.class, remap = false)
public abstract class MixinControlMode {

    @Shadow
    @Final
    private static ControlMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ControlMode[] values() {
        return $VALUES;
    }
}
