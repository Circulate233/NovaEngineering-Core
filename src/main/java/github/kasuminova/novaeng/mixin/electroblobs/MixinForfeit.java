package github.kasuminova.novaeng.mixin.electroblobs;

import electroblob.wizardry.constants.Element;
import electroblob.wizardry.constants.Tier;
import electroblob.wizardry.misc.Forfeit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(value = Forfeit.class, remap = false)
public class MixinForfeit {

    @Unique
    private static final List<String> novaEngineering_Core$banList = Arrays.asList(
            "bury_self"
    );

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private static void addMixin(Tier tier, Element element, Forfeit forfeit, CallbackInfo ci) {
        if (novaEngineering_Core$banList.contains(((AccessorForfeit) forfeit).getName().getPath())) {
            ci.cancel();
        }
    }
}
