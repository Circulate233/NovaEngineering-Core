package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.security.ISecurityTile.SecurityMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = SecurityMode.class, remap = false)
public abstract class MixinSecurityMode {

    @Shadow
    @Final
    private static SecurityMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static SecurityMode[] values() {
        return $VALUES;
    }
}
