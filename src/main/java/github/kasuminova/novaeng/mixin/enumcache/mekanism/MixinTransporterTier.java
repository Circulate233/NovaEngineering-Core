package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.TransporterTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TransporterTier.class, remap = false)
public abstract class MixinTransporterTier {

    @Shadow
    @Final
    private static TransporterTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TransporterTier[] values() {
        return $VALUES;
    }
}
