package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.items.builder.ShapeCardType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ShapeCardType.class, remap = false)
public abstract class MixinShapeCardType {

    @Shadow
    @Final
    private static ShapeCardType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ShapeCardType[] values() {
        return $VALUES;
    }
}
