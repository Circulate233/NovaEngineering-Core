package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.tile.TileTelescope.TelescopeRotation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TelescopeRotation.class, remap = false)
public abstract class MixinTelescopeRotation {

    @Shadow
    @Final
    private static TelescopeRotation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TelescopeRotation[] values() {
        return $VALUES;
    }
}
