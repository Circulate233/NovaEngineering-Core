package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.TubeTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TubeTier.class, remap = false)
public abstract class MixinTubeTier {

    @Shadow
    @Final
    private static TubeTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TubeTier[] values() {
        return $VALUES;
    }
}
