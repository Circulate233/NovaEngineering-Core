package github.kasuminova.novaeng.mixin.enumcache.aam;

import arcaios26.astraladditions.util.AAKeyBind;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AAKeyBind.class, remap = false)
public abstract class MixinAAKeyBind {

    @Shadow
    @Final
    private static AAKeyBind[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static AAKeyBind[] values() {
        return $VALUES;
    }
}
