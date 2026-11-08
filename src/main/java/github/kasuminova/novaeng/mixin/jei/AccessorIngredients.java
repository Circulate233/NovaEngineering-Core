package github.kasuminova.novaeng.mixin.jei;

import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.ingredients.Ingredients;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(value = Ingredients.class, remap = false)
public interface AccessorIngredients {
    @Accessor("inputs")
    Map<IIngredientType<?>, List<List<?>>> nova$getInputs();

    @Accessor("outputs")
    Map<IIngredientType<?>, List<List<?>>> nova$getOutputs();
}
