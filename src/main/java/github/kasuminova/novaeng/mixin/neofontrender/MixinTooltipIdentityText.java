package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.font.TooltipNbtText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "neofontrender.addons.tooltips.TooltipPreviewRenderers", remap = false)
public abstract class MixinTooltipIdentityText {
    @Redirect(method = "appendStackIdentity", at = @At(value = "INVOKE",
        target = "Ljava/lang/StringBuilder;append(Ljava/lang/Object;)Ljava/lang/StringBuilder;", ordinal = 0), require = 1)
    private static StringBuilder nova$appendNbtWithoutSubtreeStrings(final StringBuilder builder, final Object value) {
        return TooltipNbtText.append(builder, value);
    }
}
