package github.kasuminova.novaeng.mixin.enumcache.jei;

import mezz.jei.api.IRecipesGui.RecipeSearchMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RecipeSearchMode.class, remap = false)
public abstract class MixinRecipeSearchMode {

    @Shadow
    @Final
    private static RecipeSearchMode[] $VALUES;

    /**
     * @author circulation
     * @reason Return $VALUES instead of its clone.
     */
    @Overwrite(remap = false)
    public static RecipeSearchMode[] values() {
        return $VALUES;
    }
}
