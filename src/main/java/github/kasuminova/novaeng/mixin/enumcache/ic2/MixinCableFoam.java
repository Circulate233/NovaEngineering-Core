package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.block.wiring.CableFoam;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CableFoam.class, remap = false)
public abstract class MixinCableFoam {

    @Shadow
    @Final
    private static CableFoam[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static CableFoam[] values() {
        return $VALUES;
    }
}
