package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.handler.KeyTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = KeyTracker.class, remap = false)
public abstract class MixinKeyTracker {

    @Shadow
    @Final
    private static KeyTracker[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static KeyTracker[] values() {
        return $VALUES;
    }
}
