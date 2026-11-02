package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.api.network.WirelessType;

@Mixin(value = WirelessType.class, remap = false)
public abstract class MixinWirelessType {

    @Shadow
    @Final
    private static WirelessType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static WirelessType[] values() {
        return $VALUES;
    }
}
