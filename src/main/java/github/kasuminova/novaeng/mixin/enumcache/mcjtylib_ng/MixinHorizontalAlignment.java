package github.kasuminova.novaeng.mixin.enumcache.mcjtylib_ng;

import mcjty.lib.gui.layout.HorizontalAlignment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = HorizontalAlignment.class, remap = false)
public abstract class MixinHorizontalAlignment {

    @Shadow
    @Final
    private static HorizontalAlignment[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static HorizontalAlignment[] values() {
        return $VALUES;
    }
}
