package github.kasuminova.novaeng.mixin.enumcache.zerocore;

import it.zerono.mods.zerocore.lib.PropertyBlockFacings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = PropertyBlockFacings.class, remap = false)
public abstract class MixinPropertyBlockFacings {

    @Shadow
    @Final
    private static PropertyBlockFacings[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static PropertyBlockFacings[] values() {
        return $VALUES;
    }
}
