package github.kasuminova.novaeng.mixin.jei;

import github.kasuminova.novaeng.common.performance.FormattingText;
import mezz.jei.ingredients.IngredientInformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = IngredientInformation.class, remap = false)
public abstract class MixinIngredientInformation {
    /**
     * @author circulation
     * @reason Strip formatting in one pass while keeping tooltips and locale-dependent terms live.
     */
    @Overwrite
    private static String removeChatFormatting(final String text) {
        return FormattingText.strip(text);
    }
}
