package github.kasuminova.novaeng.mixin.enumcache.advancedrocketry;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import zmaster587.advancedRocketry.block.BlockLinkedHorizontalTexture.IconNames;

@Mixin(value = IconNames.class, remap = false)
public abstract class MixinIconNames {

    @Shadow
    @Final
    private static IconNames[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static IconNames[] values() {
        return $VALUES;
    }
}
