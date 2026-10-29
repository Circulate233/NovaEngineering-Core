package github.kasuminova.novaeng.mixin.enumcache.mekanism;

import mekanism.common.base.IFactory.RecipeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RecipeType.class, remap = false)
public abstract class MixinRecipeType {

    @Shadow
    @Final
    private static RecipeType[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RecipeType[] values() {
        return $VALUES;
    }
}
