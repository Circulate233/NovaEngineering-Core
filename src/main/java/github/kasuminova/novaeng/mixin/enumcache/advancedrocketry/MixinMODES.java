package github.kasuminova.novaeng.mixin.enumcache.advancedrocketry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.item.components.ItemJetpack.MODES;

@Mixin(value = MODES.class, remap = false)
public abstract class MixinMODES {

    @Shadow
    @Final
    private static MODES[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static MODES[] values() {
        return $VALUES;
    }
}
