package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tile.TileEntityGasTank.GasMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = GasMode.class, remap = false)
public abstract class MixinGasMode {

    @Shadow
    @Final
    private static GasMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static GasMode[] values() {
        return $VALUES;
    }
}
