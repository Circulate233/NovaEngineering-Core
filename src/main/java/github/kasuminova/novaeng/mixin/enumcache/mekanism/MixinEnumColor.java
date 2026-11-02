package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.api.EnumColor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumColor.class, remap = false)
public abstract class MixinEnumColor {

    @Shadow
    @Final
    private static EnumColor[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumColor[] values() {
        return $VALUES;
    }
}
