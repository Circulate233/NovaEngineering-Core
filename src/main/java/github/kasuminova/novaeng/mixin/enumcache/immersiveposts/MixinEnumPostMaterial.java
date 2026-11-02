package github.kasuminova.novaeng.mixin.enumcache.immersiveposts;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import twistedgate.immersiveposts.enums.EnumPostMaterial;

@Mixin(value = EnumPostMaterial.class, remap = false)
public abstract class MixinEnumPostMaterial {

    @Shadow
    @Final
    private static EnumPostMaterial[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumPostMaterial[] values() {
        return $VALUES;
    }
}
