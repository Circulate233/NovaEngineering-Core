package github.kasuminova.novaeng.mixin.wizardry;

import electroblob.wizardry.worldgen.WorldGenWizardryStructure;
import github.kasuminova.novaeng.common.performance.WizardryPlacementChoices;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WorldGenWizardryStructure.class, remap = false)
public abstract class MixinStructurePlacementChoices {
    @Redirect(method = "generate", at = @At(value = "INVOKE",
        target = "Lelectroblob/wizardry/worldgen/WorldGenWizardryStructure;getValidRotations()[Lnet/minecraft/util/Rotation;"), require = 1)
    private Rotation[] nova$rotations(final WorldGenWizardryStructure generator) {
        return WizardryPlacementChoices.rotations(generator);
    }

    @Redirect(method = "generate", at = @At(value = "INVOKE",
        target = "Lelectroblob/wizardry/worldgen/WorldGenWizardryStructure;getValidMirrors()[Lnet/minecraft/util/Mirror;"), require = 1)
    private Mirror[] nova$mirrors(final WorldGenWizardryStructure generator) {
        return WizardryPlacementChoices.mirrors(generator);
    }
}
