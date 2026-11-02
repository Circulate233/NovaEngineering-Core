package github.kasuminova.novaeng.mixin.enumcache.astralsorcery;

import hellfirepvp.astralsorcery.common.crafting.helper.ShapedRecipeSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ShapedRecipeSlot.class, remap = false)
public abstract class MixinShapedRecipeSlot {

    @Shadow
    @Final
    private static ShapedRecipeSlot[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static ShapedRecipeSlot[] values() {
        return $VALUES;
    }
}
