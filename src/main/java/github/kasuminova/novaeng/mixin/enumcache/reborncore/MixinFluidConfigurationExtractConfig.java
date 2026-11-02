package github.kasuminova.novaeng.mixin.enumcache.reborncore;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import reborncore.common.tile.FluidConfiguration.ExtractConfig;

@Mixin(value = ExtractConfig.class, remap = false)
public abstract class MixinFluidConfigurationExtractConfig {

    @Shadow
    @Final
    private static ExtractConfig[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ExtractConfig[] values() {
        return $VALUES;
    }
}
