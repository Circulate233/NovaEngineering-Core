package github.kasuminova.novaeng.mixin.ae2;

import appeng.client.render.FacingToRotation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FacingToRotation.class, remap = false)
public abstract class MixinFacingToRotationLookup {
    @Shadow @Final private static FacingToRotation[] $VALUES;

    @Redirect(method = "get", at = @At(value = "INVOKE",
        target = "Lappeng/client/render/FacingToRotation;values()[Lappeng/client/render/FacingToRotation;"), require = 1)
    private static FacingToRotation[] nova$lookupValues() {
        return $VALUES;
    }
}
