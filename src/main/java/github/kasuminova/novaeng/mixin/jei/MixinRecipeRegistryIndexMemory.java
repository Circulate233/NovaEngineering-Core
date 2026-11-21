package github.kasuminova.novaeng.mixin.jei;

import github.kasuminova.novaeng.common.performance.HeiIndexCapacity;
import github.kasuminova.novaeng.common.performance.HeiRecipeIdMap;
import github.kasuminova.novaeng.common.performance.HeiRecipeIndexCompaction;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.collect.ListMultiMap;
import mezz.jei.collect.Table;
import mezz.jei.recipes.RecipeMap;
import mezz.jei.recipes.RecipeRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RecipeRegistry.class, remap = false)
public abstract class MixinRecipeRegistryIndexMemory implements HeiRecipeIndexCompaction {
    @Shadow
    @Final
    private Object2LongMap<IRecipeWrapper> recipeIds;
    @Shadow
    @Final
    private Table<String, Object, IRecipeWrapper> wrapperMaps;
    @Shadow
    @Final
    private Table<IRecipeCategory, Long, IRecipeWrapper> recipeWrappersByCategory;
    @Shadow
    @Final
    private ListMultiMap<IRecipeCategory, IRecipeWrapper> recipeWrappersForCategories;
    @Shadow
    @Final
    private RecipeMap recipeInputMap;
    @Shadow
    @Final
    private RecipeMap recipeOutputMap;

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
        target = "Lmezz/jei/collect/Table;hashBasedTable()Lmezz/jei/collect/Table;"))
    private Table<IRecipeCategory, Long, IRecipeWrapper> nova$primitiveRecipeIds() {
        // Keep Table's API and row semantics while storing IDs in primitive arrays.
        return new Table<>(new Object2ObjectOpenHashMap<>(), HeiRecipeIdMap::create);
    }

    @Override
    public void nova$compactRecipeIndexes() {
        HeiIndexCapacity.trimMap(recipeIds);
        HeiIndexCapacity.trimTable(wrapperMaps);
        HeiIndexCapacity.trimTable(recipeWrappersByCategory);
        HeiIndexCapacity.trimMultiMap(recipeWrappersForCategories);
        ((HeiRecipeIndexCompaction) recipeInputMap).nova$compactRecipeIndexes();
        ((HeiRecipeIndexCompaction) recipeOutputMap).nova$compactRecipeIndexes();
    }
}
