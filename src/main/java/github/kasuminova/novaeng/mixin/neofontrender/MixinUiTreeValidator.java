package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.gui.UiValidationScratch;
import neofontrender.addons.api.ui.navigation.UiTreeSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;

@Pseudo
@Mixin(targets = "neofontrender.addons.navigation.UiTreeValidator", remap = false)
public abstract class MixinUiTreeValidator {
    /** @author NovaEngineering @reason Keep validation behavior with reusable fastutil sets. */
    @Overwrite
    public static void validate(final UiTreeSnapshot tree) {
        UiValidationScratch.validate(tree);
    }
}
