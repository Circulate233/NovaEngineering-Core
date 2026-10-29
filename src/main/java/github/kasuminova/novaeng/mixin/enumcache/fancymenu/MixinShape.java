package github.kasuminova.novaeng.mixin.enumcache.fancymenu;

import de.keksuccino.fancymenu.menu.fancy.item.ShapeCustomizationItem.Shape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Shape.class, remap = false)
public abstract class MixinShape {

    @Shadow
    @Final
    private static Shape[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static Shape[] values() {
        return $VALUES;
    }
}
