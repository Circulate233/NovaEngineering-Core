package github.kasuminova.novaeng.mixin.enumcache.enderio;

import crazypants.enderio.base.render.property.EnumDecoBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EnumDecoBlock.class, remap = false)
public abstract class MixinEnumDecoBlock {

    @Shadow
    @Final
    private static EnumDecoBlock[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static EnumDecoBlock[] values() {
        return $VALUES;
    }
}
