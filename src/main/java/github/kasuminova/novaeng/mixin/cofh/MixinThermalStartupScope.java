package github.kasuminova.novaeng.mixin.cofh;

import cofh.thermalexpansion.ThermalExpansion;
import cofh.thermalexpansion.util.managers.machine.SawmillManager;
import github.kasuminova.novaeng.common.performance.StartupCraftingScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ThermalExpansion.class, remap = false)
public abstract class MixinThermalStartupScope {
    @Redirect(method = "initManagers", at = @At(value = "INVOKE",
        target = "Lcofh/thermalexpansion/util/managers/machine/SawmillManager;initialize()V"), require = 1)
    private void nova$indexSawmillInputs() {
        try (StartupCraftingScope ignored = StartupCraftingScope.open()) {
            SawmillManager.initialize();
        }
    }
}
