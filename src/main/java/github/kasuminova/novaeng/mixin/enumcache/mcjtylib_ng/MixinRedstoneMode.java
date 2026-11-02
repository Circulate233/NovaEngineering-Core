package github.kasuminova.novaeng.mixin.enumcache.mcjtylib_ng;

import mcjty.lib.varia.RedstoneMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RedstoneMode.class, remap = false)
public abstract class MixinRedstoneMode {

    @Shadow
    @Final
    private static RedstoneMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RedstoneMode[] values() {
        return $VALUES;
    }
}
