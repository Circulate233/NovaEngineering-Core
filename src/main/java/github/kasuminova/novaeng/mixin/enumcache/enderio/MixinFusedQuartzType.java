package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.material.glass.FusedQuartzType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FusedQuartzType.class, remap = false)
public abstract class MixinFusedQuartzType {

    @Shadow
    @Final
    private static FusedQuartzType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FusedQuartzType[] values() {
        return $VALUES;
    }
}
