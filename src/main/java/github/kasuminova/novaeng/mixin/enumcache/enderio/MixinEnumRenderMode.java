package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.render.property.EnumRenderMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumRenderMode.class, remap = false)
public abstract class MixinEnumRenderMode {

    @Shadow
    @Final
    private static EnumRenderMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumRenderMode[] values() {
        return $VALUES;
    }
}
