package github.kasuminova.novaeng.mixin.enumcache.rftools;

import mcjty.rftools.shapes.ShapeOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ShapeOperation.class, remap = false)
public abstract class MixinShapeOperation {

    @Shadow
    @Final
    private static ShapeOperation[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ShapeOperation[] values() {
        return $VALUES;
    }
}
