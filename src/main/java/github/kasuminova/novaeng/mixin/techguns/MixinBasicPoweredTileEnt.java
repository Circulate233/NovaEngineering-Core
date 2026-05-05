package github.kasuminova.novaeng.mixin.techguns;

import github.kasuminova.novaeng.common.util.OnlyWriteEnergyContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import techguns.tileentities.BasicPoweredTileEnt;
import techguns.tileentities.EnergyStoragePlus;

@Mixin(value = BasicPoweredTileEnt.class, remap = false)
public class MixinBasicPoweredTileEnt extends TileEntity {

    @Shadow
    protected EnergyStoragePlus energy;
    @Unique
    private OnlyWriteEnergyContainer n$energy;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        n$energy = new OnlyWriteEnergyContainer(energy);
    }

    /**
     * @author circulation
     * @reason 仅向外暴露不可提取能量的容器
     */
    @Overwrite
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(this.n$energy.setStorage(energy)) : super.getCapability(capability, facing);
    }
}
