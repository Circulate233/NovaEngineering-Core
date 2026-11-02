package github.kasuminova.novaeng.mixin.enumcache.bibliocraft;

import jds.bibliocraft.helpers.EnumPaintingFrame;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumPaintingFrame.class, remap = false)
public abstract class MixinEnumPaintingFrame {

    @Shadow
    @Final
    private static EnumPaintingFrame[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumPaintingFrame[] values() {
        return $VALUES;
    }
}
