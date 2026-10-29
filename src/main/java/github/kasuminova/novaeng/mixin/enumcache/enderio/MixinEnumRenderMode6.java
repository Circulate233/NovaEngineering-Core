package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.render.property.EnumRenderMode6;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumRenderMode6.class, remap = false)
public abstract class MixinEnumRenderMode6 {

    @Shadow
    @Final
    private static EnumRenderMode6[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumRenderMode6[] values() {
        return $VALUES;
    }
}
