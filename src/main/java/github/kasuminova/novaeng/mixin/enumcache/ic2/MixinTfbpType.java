package github.kasuminova.novaeng.mixin.enumcache.ic2;

import ic2.core.item.tfbp.Tfbp.TfbpType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TfbpType.class, remap = false)
public abstract class MixinTfbpType {

    @Shadow
    @Final
    private static TfbpType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static TfbpType[] values() {
        return $VALUES;
    }
}
