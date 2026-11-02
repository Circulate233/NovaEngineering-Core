package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.blocks.shield.ShieldRenderingMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ShieldRenderingMode.class, remap = false)
public abstract class MixinShieldRenderingMode {

    @Shadow
    @Final
    private static ShieldRenderingMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ShieldRenderingMode[] values() {
        return $VALUES;
    }
}
