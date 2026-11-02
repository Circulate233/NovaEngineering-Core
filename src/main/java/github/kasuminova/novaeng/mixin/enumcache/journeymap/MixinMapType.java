package github.kasuminova.novaeng.mixin.enumcache.journeymap;

import journeymap.api.v2.common.Context.MapType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = MapType.class, remap = false)
public abstract class MixinMapType {

    @Shadow
    @Final
    private static MapType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MapType[] values() {
        return $VALUES;
    }
}
