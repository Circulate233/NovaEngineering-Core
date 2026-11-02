package github.kasuminova.novaeng.mixin.enumcache.ctm;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import team.chisel.ctm.client.util.ConnectionLocations;

@Mixin(value = ConnectionLocations.class, remap = false)
public abstract class MixinConnectionLocations {

    @Shadow
    @Final
    private static ConnectionLocations[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ConnectionLocations[] values() {
        return $VALUES;
    }
}
