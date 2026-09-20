package github.kasuminova.novaeng.mixin.astralsorcery;

import hellfirepvp.astralsorcery.client.util.resource.BindableResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Defers Astral Sorcery texture allocation until the resource is actually bound. */
@Mixin(value = BindableResource.class, remap = false)
public abstract class MixinBindableResourceLazyAllocation {

    @Redirect(
        method = "<init>",
        at = @At(value = "INVOKE",
            target = "Lhellfirepvp/astralsorcery/client/util/resource/BindableResource;allocateGlId()V",
            remap = false),
        require = 1)
    private void nova$deferInitialAllocation(final BindableResource resource) {
    }
}
