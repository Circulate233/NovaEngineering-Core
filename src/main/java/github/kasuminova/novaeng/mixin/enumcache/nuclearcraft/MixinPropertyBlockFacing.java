package github.kasuminova.novaeng.mixin.enumcache.nuclearcraft;

import nc.multiblock.internal.PropertyBlockFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PropertyBlockFacing.class, remap = false)
public abstract class MixinPropertyBlockFacing {

    @Shadow
    @Final
    private static PropertyBlockFacing[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PropertyBlockFacing[] values() {
        return $VALUES;
    }
}
