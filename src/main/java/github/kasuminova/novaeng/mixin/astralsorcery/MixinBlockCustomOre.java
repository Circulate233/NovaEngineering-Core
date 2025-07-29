package github.kasuminova.novaeng.mixin.astralsorcery;

import hellfirepvp.astralsorcery.common.block.BlockCustomOre;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockCustomOre.class,remap = false)
public class MixinBlockCustomOre {

    @Inject(method = "canSilkHarvest",at = @At("HEAD"), cancellable = true)
    public void canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "securityCheck",at = @At("HEAD"), cancellable = true)
    private void securityCheck(World world, EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!world.isRemote);
    }

    @Inject(method = "checkSafety",at = @At("HEAD"), cancellable = true)
    private void checkSafety(World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

}
