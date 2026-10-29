package github.kasuminova.novaeng.mixin.enumcache.ancientspellcraft;

import com.windanesz.ancientspellcraft.constants.EnumFacingIntercardinal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumFacingIntercardinal.class, remap = false)
public abstract class MixinEnumFacingIntercardinal {

    @Shadow
    @Final
    private static EnumFacingIntercardinal[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumFacingIntercardinal[] values() {
        return $VALUES;
    }
}
