package github.kasuminova.novaeng.mixin.enumcache.ebwizardry;

import electroblob.wizardry.constants.Tier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Tier.class, remap = false)
public abstract class MixinTier {

    @Shadow
    @Final
    private static Tier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Tier[] values() {
        return $VALUES;
    }
}
