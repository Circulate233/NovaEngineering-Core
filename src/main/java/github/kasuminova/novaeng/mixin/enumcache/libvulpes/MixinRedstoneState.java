package github.kasuminova.novaeng.mixin.enumcache.libvulpes;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.libVulpes.util.ZUtils.RedstoneState;

@Mixin(value = RedstoneState.class, remap = false)
public abstract class MixinRedstoneState {

    @Shadow
    @Final
    private static RedstoneState[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RedstoneState[] values() {
        return $VALUES;
    }
}
