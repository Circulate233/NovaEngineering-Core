package github.kasuminova.novaeng.mixin.enumcache.customloadingscreen;

import alexiil.mc.mod.load.progress.SingleProgressBarTracker.ReloadPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ReloadPart.class, remap = false)
public abstract class MixinReloadPart {

    @Shadow
    @Final
    private static ReloadPart[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ReloadPart[] values() {
        return $VALUES;
    }
}
