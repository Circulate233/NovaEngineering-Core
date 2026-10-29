package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.item.ItemConfigurator.ConfiguratorMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ConfiguratorMode.class, remap = false)
public abstract class MixinConfiguratorMode {

    @Shadow
    @Final
    private static ConfiguratorMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConfiguratorMode[] values() {
        return $VALUES;
    }
}
