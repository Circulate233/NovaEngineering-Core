package github.kasuminova.novaeng.mixin.enumcache.mcjtylib_ng;

import mcjty.lib.gui.layout.VerticalAlignment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = VerticalAlignment.class, remap = false)
public abstract class MixinVerticalAlignment {

    @Shadow
    @Final
    private static VerticalAlignment[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static VerticalAlignment[] values() {
        return $VALUES;
    }
}
