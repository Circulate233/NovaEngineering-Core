package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.util.NovaRAB;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipeAdapterBuilder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = RecipeAdapterBuilder.class,remap = false)
public class MixinRecipeAdapterBuilder implements NovaRAB {

    @Unique
    public String novaEngineering_Core$inTags = "";
    @Unique
    public String novaEngineering_Core$outTags = "";

    @Unique
    @Override
    public void n$setInTags(String tagName) {
        this.novaEngineering_Core$inTags = tagName;
    }

    @Unique
    @Override
    public String n$getInTags() {
        return novaEngineering_Core$inTags;
    }

    @Unique
    @Override
    public void n$setOutTags(@NotNull String tagName) {
        this.novaEngineering_Core$outTags = tagName;
    }

    @Unique
    @Override
    public String n$getOutTags() {
        return novaEngineering_Core$outTags;
    }

}
