package github.kasuminova.novaeng.mixin.minecraft;

import net.minecraft.block.BlockLiquid;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockLiquid.class)
public abstract class MixinLiquidMixingPosition {

    @Unique
    private static final ThreadLocal<BlockPos.MutableBlockPos> NOVA_LIQUID_MIXING_POSITION =
        ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    @Redirect(
        method = "checkForMixing(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/EnumFacing;values()[Lnet/minecraft/util/EnumFacing;"
        ),
        require = 1
    )
    private EnumFacing[] nova$reuseMixingDirections() {
        return EnumFacing.VALUES;
    }

    @Redirect(
        method = "checkForMixing(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;offset(Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/util/math/BlockPos;"
        ),
        require = 1
    )
    private BlockPos nova$reuseNeighborPosition(final BlockPos origin, final EnumFacing facing) {
        return NOVA_LIQUID_MIXING_POSITION.get().setPos(origin).move(facing);
    }
}
