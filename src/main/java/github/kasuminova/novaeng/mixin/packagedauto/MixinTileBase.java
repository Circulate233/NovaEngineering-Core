package github.kasuminova.novaeng.mixin.packagedauto;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thelm.packagedauto.energy.EnergyStorage;
import thelm.packagedauto.tile.TileBase;

@Mixin(value = TileBase.class,remap = false)
public abstract class MixinTileBase {

    @Shadow
    public abstract void setEnergyStorage(EnergyStorage energyStorage);

    @Shadow
    protected EnergyStorage energyStorage;

    @Inject(method = "<init>",at = @At("TAIL"))
    public void onConstructed(CallbackInfo ci){
        setEnergyStorage(energyStorage);
    }

    @Inject(method = "setEnergyStorage",at = @At("HEAD"))
    public void onSetEnergyStorage(EnergyStorage energyStorage, CallbackInfo ci){
        energyStorage.setMaxExtract(0);
    }

}
