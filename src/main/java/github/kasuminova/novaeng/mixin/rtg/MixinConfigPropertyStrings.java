package github.kasuminova.novaeng.mixin.rtg;

import github.kasuminova.novaeng.common.performance.string.StringCanon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "rtg.api.config.property.ConfigProperty", remap = false)
public abstract class MixinConfigPropertyStrings {
    @Shadow(remap = false)
    private String description;

    @Inject(method = {"<init>", "formatDescription"}, at = @At("RETURN"))
    private void nova$shareDescription(final CallbackInfo ci) {
        description = StringCanon.value(description);
    }
}
