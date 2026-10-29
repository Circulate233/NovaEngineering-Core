package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.api.screens.FormatStyle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FormatStyle.class, remap = false)
public abstract class MixinFormatStyle {

    @Shadow
    @Final
    private static FormatStyle[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FormatStyle[] values() {
        return $VALUES;
    }
}
