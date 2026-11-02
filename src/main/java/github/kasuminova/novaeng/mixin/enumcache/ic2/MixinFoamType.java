package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.block.BlockFoam.FoamType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = FoamType.class, remap = false)
public abstract class MixinFoamType {

    @Shadow
    @Final
    private static FoamType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static FoamType[] values() {
        return $VALUES;
    }
}
