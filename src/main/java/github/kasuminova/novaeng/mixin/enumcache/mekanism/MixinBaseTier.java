package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.tier.BaseTier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BaseTier.class, remap = false)
public abstract class MixinBaseTier {

    @Shadow
    @Final
    private static BaseTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static BaseTier[] values() {
        return $VALUES;
    }
}
