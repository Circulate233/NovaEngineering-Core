package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.FluidTankTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FluidTankTier.class, remap = false)
public abstract class MixinFluidTankTier {

    @Shadow
    @Final
    private static FluidTankTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FluidTankTier[] values() {
        return $VALUES;
    }
}
