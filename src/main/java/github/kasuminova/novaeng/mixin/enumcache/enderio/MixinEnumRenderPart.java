package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.render.property.EnumRenderPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumRenderPart.class, remap = false)
public abstract class MixinEnumRenderPart {

    @Shadow
    @Final
    private static EnumRenderPart[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumRenderPart[] values() {
        return $VALUES;
    }
}
