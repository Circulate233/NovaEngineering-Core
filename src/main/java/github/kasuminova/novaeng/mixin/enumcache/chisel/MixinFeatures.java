package github.kasuminova.novaeng.mixin.enumcache.chisel;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import team.chisel.Features;

@Mixin(value = Features.class, remap = false)
public abstract class MixinFeatures {

    @Shadow
    @Final
    private static Features[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Features[] values() {
        return $VALUES;
    }
}
