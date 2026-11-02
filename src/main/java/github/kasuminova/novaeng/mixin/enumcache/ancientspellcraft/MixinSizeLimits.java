package github.kasuminova.novaeng.mixin.enumcache.ancientspellcraft;

import com.windanesz.ancientspellcraft.ritual.ArcaneBarrier.SizeLimits;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SizeLimits.class, remap = false)
public abstract class MixinSizeLimits {

    @Shadow
    @Final
    private static SizeLimits[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SizeLimits[] values() {
        return $VALUES;
    }
}
