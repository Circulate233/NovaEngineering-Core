package github.kasuminova.novaeng.mixin.mmce;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import github.kasuminova.novaeng.common.performance.PreviewIngredientList;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Mixin(value = BlockArray.class, remap = false)
public abstract class MixinBlockArrayIngredients {

    @Redirect(method = "lambda$getIngredientList$2", at = @At(value = "INVOKE",
        target = "Ljava/util/List;iterator()Ljava/util/Iterator;"), require = 1)
    private static Iterator<List<ItemStack>> nova$matchingItemCandidates(final List<List<ItemStack>> ingredients,
                                                                         @Local(name = "input") final ItemStack input) {
        // The original loop still checks metadata, NBT and capabilities and grows its first match.
        return ingredients instanceof PreviewIngredientList indexed
            ? indexed.candidates(input) : ingredients.iterator();
    }

    @Redirect(method = "getIngredientList", at = @At(value = "NEW", target = "java/util/LinkedList"), require = 1)
    private LinkedList<List<ItemStack>> nova$indexedIngredients() {
        return new PreviewIngredientList();
    }

    @ModifyReturnValue(method = "getIngredientList", at = @At("RETURN"), require = 1)
    private List<List<ItemStack>> nova$releaseIndex(final List<List<ItemStack>> ingredients) {
        if (ingredients instanceof PreviewIngredientList indexed) {
            indexed.finishBuilding();
        }
        return ingredients;
    }
}
