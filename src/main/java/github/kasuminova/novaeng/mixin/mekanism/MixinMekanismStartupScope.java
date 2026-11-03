package github.kasuminova.novaeng.mixin.mekanism;

import github.kasuminova.novaeng.common.performance.StartupCraftingScope;
import mekanism.common.integration.OreDictManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = OreDictManager.class, remap = false)
public abstract class MixinMekanismStartupScope {
    @Shadow
    private static void addLogRecipes() {
        throw new AssertionError();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE",
        target = "Lmekanism/common/integration/OreDictManager;addLogRecipes()V"), require = 1)
    private static void nova$indexLogRecipes() {
        try (StartupCraftingScope ignored = StartupCraftingScope.open()) {
            addLogRecipes();
        }
    }
}
