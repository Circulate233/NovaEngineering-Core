package github.kasuminova.novaeng.mixin.nco;

import nc.ncpf.NCPFWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips NuclearCraft's generated NCPF export when no consumer needs the file.
 * Set {@link #NOVA$SKIP_UNUSED_EXPORT} to {@code false} to restore the original export.
 */
@Mixin(value = NCPFWriter.class, remap = false)
public class MixinNCPFWriter {

    @Unique
    private static final boolean NOVA$SKIP_UNUSED_EXPORT = true;

    @Inject(method = "exportNCPF", at = @At("HEAD"), cancellable = true,
        require = 1)
    private static void nova$skipUnusedExport(final CallbackInfo ci) {
        if (NOVA$SKIP_UNUSED_EXPORT) {
            ci.cancel();
        }
    }
}
