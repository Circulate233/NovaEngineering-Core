package github.kasuminova.novaeng.mixin.mekanism;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Cancellable;
import mekanism.common.block.BlockMachine;
import mekanism.common.tile.prefab.TileEntityBasicBlock;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockMachine.class, remap = false)
public abstract class MixinBlockMachineDisplayTick {

    // A stale display tick can observe a replacement tile before the original CHECKCAST.
    @ModifyExpressionValue(method = "randomDisplayTick(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getTileEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/tileentity/TileEntity;"),
        remap = true, require = 1)
    private TileEntity nova$validateDisplayTile(final TileEntity tile, @Cancellable final CallbackInfo ci) {
        if (!(tile instanceof TileEntityBasicBlock)) ci.cancel();
        return tile;
    }
}
