package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.PipeTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PipeTier.class, remap = false)
public abstract class MixinPipeTier {

    @Shadow
    @Final
    private static PipeTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PipeTier[] values() {
        return $VALUES;
    }
}
