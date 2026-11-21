package github.kasuminova.novaeng.mixin.mmce;

import github.kasuminova.novaeng.common.performance.string.StringCanon;
import hellfirepvp.modularmachinery.common.crafting.MachineRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MachineRecipe.class, remap = false)
public abstract class MixinMachineRecipeStrings {
    @Shadow
    @Final
    @Mutable
    protected String recipeFilePath;
    @Shadow
    protected String threadName;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nova$shareRecipeMetadata(final CallbackInfo ci) {
        recipeFilePath = StringCanon.value(recipeFilePath);
        threadName = StringCanon.value(threadName);
    }
}
