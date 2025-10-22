package github.kasuminova.novaeng.mixin.libvulpes;

import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zmaster587.libVulpes.tile.multiblock.hatch.TileFluidHatch;

@Mixin(value = TileFluidHatch.class, remap = false)
public class MixinTileFluidHatch {

    @Inject(method = "drain(Lnet/minecraftforge/fluids/FluidStack;Z)Lnet/minecraftforge/fluids/FluidStack;", at = @At("HEAD"), cancellable = true)
    public void drainMixin(FluidStack resource, boolean doDrain, CallbackInfoReturnable<FluidStack> cir) {
        if (resource == null) {
            cir.setReturnValue(null);
        }
    }
}
