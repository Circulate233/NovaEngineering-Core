package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.FactoryTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FactoryTier.class, remap = false)
public abstract class MixinFactoryTier {

    @Shadow
    @Final
    private static FactoryTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FactoryTier[] values() {
        return $VALUES;
    }
}
