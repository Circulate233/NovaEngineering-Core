package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.util.UnitDisplayUtils.TempType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TempType.class, remap = false)
public abstract class MixinTempType {

    @Shadow
    @Final
    private static TempType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TempType[] values() {
        return $VALUES;
    }
}
