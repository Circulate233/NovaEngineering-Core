package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.util.AEPartLocation;
import appeng.parts.CableBusContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CableBusContainer.class, remap = false)
public abstract class MixinCableBusLightValues {
    @Unique private static final AEPartLocation[] nova$lightLocations = AEPartLocation.values();

    @Redirect(method = "getLightValue", at = @At(value = "INVOKE",
        target = "Lappeng/api/util/AEPartLocation;values()[Lappeng/api/util/AEPartLocation;"), require = 1)
    private AEPartLocation[] nova$lightValues() {
        return nova$lightLocations;
    }
}
