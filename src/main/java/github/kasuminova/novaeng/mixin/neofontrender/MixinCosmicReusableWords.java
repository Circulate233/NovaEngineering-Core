package github.kasuminova.novaeng.mixin.neofontrender;

import github.kasuminova.novaeng.client.font.BoundedStringLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "neofontrender.core.font.cosmic.CosmicTextSegmenter", remap = false)
public abstract class MixinCosmicReusableWords {
    @Unique
    private static final BoundedStringLists nova$words = new BoundedStringLists();

    @Inject(method = "splitReusableWords", at = @At("HEAD"), cancellable = true, require = 1)
    private static void nova$reuseSegments(final String text, final CallbackInfoReturnable<List<String>> callback) {
        final List<String> cached = nova$words.get(text);
        if (cached != null) callback.setReturnValue(cached);
    }

    @Inject(method = "splitReusableWords", at = @At("RETURN"), require = 1)
    private static void nova$rememberSegments(final String text, final CallbackInfoReturnable<List<String>> callback) {
        nova$words.put(text, callback.getReturnValue());
    }
}
