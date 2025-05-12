package github.kasuminova.novaeng.mixin.nco;

import nc.tile.fluid.ITileFluid;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ITileFluid.class,remap = false)
public interface MixinITileFluid {

    @Inject(method = "drain(Lnet/minecraft/util/EnumFacing;Lnet/minecraftforge/fluids/FluidStack;Z)Lnet/minecraftforge/fluids/FluidStack;",at = @At("HEAD"), cancellable = true)
    default void drainMixin(EnumFacing side, FluidStack resource, boolean doDrain, CallbackInfoReturnable<FluidStack> cir){
        if (resource == null){
            cir.setReturnValue(null);
        }
    }

}
