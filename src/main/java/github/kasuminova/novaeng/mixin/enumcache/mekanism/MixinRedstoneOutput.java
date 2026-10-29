package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tile.laser.TileEntityLaserAmplifier.RedstoneOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RedstoneOutput.class, remap = false)
public abstract class MixinRedstoneOutput {

    @Shadow
    @Final
    private static RedstoneOutput[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RedstoneOutput[] values() {
        return $VALUES;
    }
}
