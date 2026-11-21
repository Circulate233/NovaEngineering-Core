package github.kasuminova.novaeng.mixin.codechicken;

import github.kasuminova.novaeng.common.performance.string.StringCanon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Pseudo
@Mixin(targets = {"codechicken.lib.reflect.ObfMapping$ObfRemapper", "gregtech.asm.util.ObfMapping$ObfRemapper",
    "com.cleanroommc.multiblocked.core.asm.util.ObfMapping$ObfRemapper"}, remap = false)
public abstract class MixinObfRemapperStrings {
    @Shadow(remap = false)
    private HashMap<String, String> fields;
    @Shadow(remap = false)
    private HashMap<String, String> funcs;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nova$shareDictionaryValues(final CallbackInfo ci) {
        fields.replaceAll((key, value) -> StringCanon.value(value));
        funcs.replaceAll((key, value) -> StringCanon.value(value));
    }
}
