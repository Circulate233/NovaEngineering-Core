package github.kasuminova.novaeng.mixin.enumcache.journeymap;

import journeymap.common.network.model.ServerPropertyType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ServerPropertyType.class, remap = false)
public abstract class MixinServerPropertyType {

    @Shadow
    @Final
    private static ServerPropertyType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ServerPropertyType[] values() {
        return $VALUES;
    }
}
