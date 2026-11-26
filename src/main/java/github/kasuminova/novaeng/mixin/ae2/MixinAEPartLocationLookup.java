package github.kasuminova.novaeng.mixin.ae2;

import appeng.api.util.AEPartLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AEPartLocation.class, remap = false)
public abstract class MixinAEPartLocationLookup {
    @Shadow @Final private static AEPartLocation[] $VALUES;

    @Redirect(method = "fromFacing", at = @At(value = "INVOKE",
        target = "Lappeng/api/util/AEPartLocation;values()[Lappeng/api/util/AEPartLocation;"), require = 1)
    private static AEPartLocation[] nova$lookupValues() {
        // The sole consumer indexes this array. Public values() retains its clone semantics.
        return $VALUES;
    }
}
