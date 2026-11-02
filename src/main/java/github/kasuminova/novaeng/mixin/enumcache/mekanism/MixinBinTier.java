package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.BinTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BinTier.class, remap = false)
public abstract class MixinBinTier {

    @Shadow
    @Final
    private static BinTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BinTier[] values() {
        return $VALUES;
    }
}
