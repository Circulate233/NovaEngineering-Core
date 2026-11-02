package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.material.alloy.Alloy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Alloy.class, remap = false)
public abstract class MixinAlloy {

    @Shadow
    @Final
    private static Alloy[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Alloy[] values() {
        return $VALUES;
    }
}
