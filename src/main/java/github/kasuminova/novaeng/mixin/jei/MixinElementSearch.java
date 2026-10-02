package github.kasuminova.novaeng.mixin.jei;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import mezz.jei.api.search.ISearchIndexBuilder;
import mezz.jei.gui.ingredients.IIngredientListElement;
import mezz.jei.search.AsyncPrefixedSearchable;
import mezz.jei.search.ElementSearch;
import mezz.jei.search.PrefixInfo;
import mezz.jei.search.PrefixedSearchable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ElementSearch.class, remap = false)
public abstract class MixinElementSearch {

    @Redirect(method = "<init>",
              at = @At(value = "NEW",
                       target = "(Lmezz/jei/api/search/ISearchIndexBuilder;Lmezz/jei/search/PrefixInfo;)Lmezz/jei/search/PrefixedSearchable;"),
              remap = false)
    private PrefixedSearchable nova$asyncSearchIndex(final ISearchIndexBuilder<IIngredientListElement<?>> searchIndexBuilder,
                                                     final PrefixInfo prefixInfo) {
        if (NovaEngCoreConfig.CLIENT.optimizeJeiAsyncSearchIndex) {
            return new AsyncPrefixedSearchable(searchIndexBuilder, prefixInfo);
        }
        return new PrefixedSearchable(searchIndexBuilder, prefixInfo);
    }
}
