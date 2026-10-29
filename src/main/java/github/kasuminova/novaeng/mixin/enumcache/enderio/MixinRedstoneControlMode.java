package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.machine.modes.RedstoneControlMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RedstoneControlMode.class, remap = false)
public abstract class MixinRedstoneControlMode {

    @Shadow
    @Final
    private static RedstoneControlMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RedstoneControlMode[] values() {
        return $VALUES;
    }
}
