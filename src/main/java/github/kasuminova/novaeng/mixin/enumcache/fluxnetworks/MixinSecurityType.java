package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.api.network.SecurityType;

@Mixin(value = SecurityType.class, remap = false)
public abstract class MixinSecurityType {

    @Shadow
    @Final
    private static SecurityType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SecurityType[] values() {
        return $VALUES;
    }
}
