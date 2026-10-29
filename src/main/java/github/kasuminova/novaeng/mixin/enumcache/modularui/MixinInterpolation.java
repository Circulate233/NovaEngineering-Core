package github.kasuminova.novaeng.mixin.enumcache.modularui;

import com.cleanroommc.modularui.utils.Interpolation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Interpolation.class, remap = false)
public abstract class MixinInterpolation {

    @Shadow
    @Final
    private static Interpolation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Interpolation[] values() {
        return $VALUES;
    }
}
