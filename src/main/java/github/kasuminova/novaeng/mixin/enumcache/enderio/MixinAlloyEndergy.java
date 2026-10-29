package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.material.alloy.endergy.AlloyEndergy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AlloyEndergy.class, remap = false)
public abstract class MixinAlloyEndergy {

    @Shadow
    @Final
    private static AlloyEndergy[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AlloyEndergy[] values() {
        return $VALUES;
    }
}
