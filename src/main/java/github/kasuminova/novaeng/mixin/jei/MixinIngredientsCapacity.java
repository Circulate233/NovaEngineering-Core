package github.kasuminova.novaeng.mixin.jei;

import github.kasuminova.novaeng.common.performance.IngredientMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import mezz.jei.ingredients.Ingredients;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * Keep singleton ingredient types small without changing multi-type callback order.
 */
@Mixin(value = Ingredients.class, remap = false)
public abstract class MixinIngredientsCapacity {
    @Redirect(method = "<init>", at = @At(value = "NEW",
        target = "it/unimi/dsi/fastutil/objects/Reference2ObjectOpenHashMap"), require = 2, allow = 2)
    private Reference2ObjectOpenHashMap<?, ?> nova$smallTypeMap() {
        return new Reference2ObjectOpenHashMap<>(1);
    }

    @Redirect(method = {
        "setInputLists(Lmezz/jei/api/recipe/IIngredientType;Ljava/util/List;)V",
        "setInputs(Lmezz/jei/api/recipe/IIngredientType;Ljava/util/List;)V",
        "setOutputs(Lmezz/jei/api/recipe/IIngredientType;Ljava/util/List;)V",
        "setOutputLists(Lmezz/jei/api/recipe/IIngredientType;Ljava/util/List;)V"
    }, at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
        require = 4, allow = 4)
    private Object nova$putType(final Map<Object, Object> map, final Object type, final Object groups) {
        return IngredientMaps.put(map, type, groups);
    }
}
