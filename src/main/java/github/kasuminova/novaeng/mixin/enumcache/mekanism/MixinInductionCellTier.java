package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.InductionCellTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = InductionCellTier.class, remap = false)
public abstract class MixinInductionCellTier {

    @Shadow
    @Final
    private static InductionCellTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static InductionCellTier[] values() {
        return $VALUES;
    }
}
