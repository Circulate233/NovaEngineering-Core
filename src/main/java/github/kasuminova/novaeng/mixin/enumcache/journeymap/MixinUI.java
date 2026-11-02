package github.kasuminova.novaeng.mixin.enumcache.journeymap;

import journeymap.api.v2.common.Context.UI;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = UI.class, remap = false)
public abstract class MixinUI {

    @Shadow
    @Final
    private static UI[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static UI[] values() {
        return $VALUES;
    }
}
