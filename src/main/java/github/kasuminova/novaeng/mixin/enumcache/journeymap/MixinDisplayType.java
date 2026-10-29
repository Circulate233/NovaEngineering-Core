package github.kasuminova.novaeng.mixin.enumcache.journeymap;

import journeymap.api.v2.client.display.DisplayType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DisplayType.class, remap = false)
public abstract class MixinDisplayType {

    @Shadow
    @Final
    private static DisplayType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static DisplayType[] values() {
        return $VALUES;
    }
}
