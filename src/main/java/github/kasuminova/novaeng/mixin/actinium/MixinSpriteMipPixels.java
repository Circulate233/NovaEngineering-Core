package github.kasuminova.novaeng.mixin.actinium;

import github.kasuminova.novaeng.client.memory.OwnedAnimationFrames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.BiFunction;

/** Older Actinium versions do not expose the scoped pixel editor. */
@Pseudo
@Mixin(targets = "com.dhj.actinium.texture.SpriteMipPixels", remap = false)
public abstract class MixinSpriteMipPixels {
    @Inject(method = "editBaseLevels", at = @At("HEAD"), cancellable = true, require = 1)
    private static <S> void nova$editOwnedPixels(final List<int[][]> frames, final S state,
                                                 final BiFunction<S, int[], S> editor,
                                                 final CallbackInfoReturnable<S> callback) {
        if (frames instanceof OwnedAnimationFrames owned) {
            callback.setReturnValue(owned.editBaseLevels(state, editor));
        }
    }
}
