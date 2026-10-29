package github.kasuminova.novaeng.mixin.enumcache.biomesoplenty;

import biomesoplenty.common.remote.TrailManager.TrailVisibilityMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TrailVisibilityMode.class, remap = false)
public abstract class MixinTrailVisibilityMode {

    @Shadow
    @Final
    private static TrailVisibilityMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TrailVisibilityMode[] values() {
        return $VALUES;
    }
}
