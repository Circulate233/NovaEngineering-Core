package github.kasuminova.novaeng.mixin.enumcache.fluxnetworks;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import sonar.fluxnetworks.api.network.ConnectionType;

@Mixin(value = ConnectionType.class, remap = false)
public abstract class MixinConnectionType {

    @Shadow
    @Final
    private static ConnectionType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConnectionType[] values() {
        return $VALUES;
    }
}
