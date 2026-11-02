package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.compat.xnet.StorageConnectorSettings.Mode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Mode.class, remap = false)
public abstract class MixinStorageConnectorSettingsMode {

    @Shadow
    @Final
    private static Mode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Mode[] values() {
        return $VALUES;
    }
}
