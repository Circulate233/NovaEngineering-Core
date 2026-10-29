package github.kasuminova.novaeng.mixin.enumcache.theoneprobe;

import mcjty.theoneprobe.api.ElementAlignment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ElementAlignment.class, remap = false)
public abstract class MixinElementAlignment {

    @Shadow
    @Final
    private static ElementAlignment[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ElementAlignment[] values() {
        return $VALUES;
    }
}
