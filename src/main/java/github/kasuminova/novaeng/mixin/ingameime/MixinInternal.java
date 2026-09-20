package github.kasuminova.novaeng.mixin.ingameime;

import com.dhj.ingameime.Internal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Drops the explicit full garbage collection performed at the end of native input context creation.
 *
 * <p>The call is the last statement of {@code createInputCtx}, after every native callback has been
 * registered, so skipping it cannot leave the context half initialised. It costs over a second of main
 * thread stall during startup while the crate's own cleanup already runs on the next collection the JVM
 * decides to perform. {@code System.gc()} is only a hint, so removing it does not change correctness.</p>
 */
@Mixin(value = Internal.class, remap = false)
public class MixinInternal {

    @Redirect(method = "createInputCtx", at = @At(value = "INVOKE",
        target = "Ljava/lang/System;gc()V", remap = false), require = 1)
    private static void nova$skipExplicitGc() {
        // Intentionally empty.
    }
}
