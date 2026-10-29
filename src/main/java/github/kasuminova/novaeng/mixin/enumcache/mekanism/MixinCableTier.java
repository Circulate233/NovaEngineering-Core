package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.CableTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CableTier.class, remap = false)
public abstract class MixinCableTier {

    @Shadow
    @Final
    private static CableTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CableTier[] values() {
        return $VALUES;
    }
}
