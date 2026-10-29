package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.filter.item.PowerItemFilter.CmpMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CmpMode.class, remap = false)
public abstract class MixinCmpMode {

    @Shadow
    @Final
    private static CmpMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CmpMode[] values() {
        return $VALUES;
    }
}
