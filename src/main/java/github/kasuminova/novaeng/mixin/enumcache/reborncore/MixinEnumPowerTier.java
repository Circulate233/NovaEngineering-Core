package github.kasuminova.novaeng.mixin.enumcache.reborncore;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import reborncore.api.power.EnumPowerTier;

@Mixin(value = EnumPowerTier.class, remap = false)
public abstract class MixinEnumPowerTier {

    @Shadow
    @Final
    private static EnumPowerTier[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumPowerTier[] values() {
        return $VALUES;
    }
}
