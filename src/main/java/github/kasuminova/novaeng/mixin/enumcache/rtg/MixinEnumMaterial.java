package github.kasuminova.novaeng.mixin.enumcache.rtg;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import rtg.api.util.BlockUtil.EnumMaterial;

@Mixin(value = EnumMaterial.class, remap = false)
public abstract class MixinEnumMaterial {

    @Shadow
    @Final
    private static EnumMaterial[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumMaterial[] values() {
        return $VALUES;
    }
}
