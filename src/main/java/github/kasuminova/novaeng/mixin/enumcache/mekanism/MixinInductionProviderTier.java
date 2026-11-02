package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.InductionProviderTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = InductionProviderTier.class, remap = false)
public abstract class MixinInductionProviderTier {

    @Shadow
    @Final
    private static InductionProviderTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static InductionProviderTier[] values() {
        return $VALUES;
    }
}
