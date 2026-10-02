package github.kasuminova.novaeng.mixin.draconicevolution;

import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyStorageCore;
import github.kasuminova.novaeng.common.integration.DECoreBindingIndex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEnergyStorageCore.class, remap = false)
public abstract class MixinTileEnergyStorageCoreBinding {

    @Inject(method = "activateCore", at = @At("RETURN"), remap = false)
    private void novaeng$activated(final CallbackInfo ci) {
        DECoreBindingIndex.updateCoreState((TileEnergyStorageCore) (Object) this);
    }

    @Inject(method = "deactivateCore", at = @At("RETURN"), remap = false)
    private void novaeng$deactivated(final CallbackInfo ci) {
        DECoreBindingIndex.updateCoreState((TileEnergyStorageCore) (Object) this);
    }

    @Inject(method = "validateStructure", at = @At("RETURN"), remap = false)
    private void novaeng$structureValidated(CallbackInfoReturnable<Boolean> cir) {
        DECoreBindingIndex.updateCoreState((TileEnergyStorageCore) (Object) this);
    }
}
