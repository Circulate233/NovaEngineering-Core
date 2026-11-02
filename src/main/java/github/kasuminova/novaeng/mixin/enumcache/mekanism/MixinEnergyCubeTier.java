package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.EnergyCubeTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnergyCubeTier.class, remap = false)
public abstract class MixinEnergyCubeTier {

    @Shadow
    @Final
    private static EnergyCubeTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnergyCubeTier[] values() {
        return $VALUES;
    }
}
