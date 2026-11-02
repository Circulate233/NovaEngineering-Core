package github.kasuminova.novaeng.mixin.astralsorcery;

import github.kasuminova.novaeng.common.performance.AstralItemComparison;
import hellfirepvp.astralsorcery.common.util.CraftingRecipeHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(value = CraftingRecipeHelper.class, remap = false)
public abstract class MixinCraftingRecipeHelper {
    /**
     * @author circulation
     * @reason 固定比较条件直接传位掩码，保留配方遍历顺序并避免逐配方参数数组和链表节点。
     */
    @Overwrite
    public static List<IRecipe> findRecipesWithOutput(final ItemStack output) {
        final ObjectArrayList<IRecipe> matches = new ObjectArrayList<>();
        for (final IRecipe recipe : ForgeRegistries.RECIPES) {
            if (AstralItemComparison.compare(output, recipe.getRecipeOutput(),
                AstralItemComparison.ITEM | AstralItemComparison.META_STRICT)) matches.add(recipe);
        }
        return matches;
    }
}
