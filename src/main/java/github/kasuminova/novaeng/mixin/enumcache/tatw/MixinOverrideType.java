package github.kasuminova.novaeng.mixin.enumcache.tatw;

import aroma1997.tatw.Config.OverrideType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = OverrideType.class, remap = false)
public abstract class MixinOverrideType {

    @Shadow
    @Final
    private static OverrideType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static OverrideType[] values() {
        return $VALUES;
    }
}
