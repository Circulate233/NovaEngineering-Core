package github.kasuminova.novaeng.mixin.ctm;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.render.DirectionalOffsetCache;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.chisel.ctm.client.util.CTMLogic;

@Mixin(value = CTMLogic.class, remap = false)
public abstract class MixinCtmLogicConnectionPosCache {

    @Redirect(method = "isConnected(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/block/state/IBlockState;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;offset(Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/util/math/BlockPos;", remap = true),
        remap = false, require = 1)
    private BlockPos nova$reuseConnectionPosition(final BlockPos position, final EnumFacing facing) {
        if (!NovaEngCoreConfig.CLIENT.optimizeChiselCtmConnectionOffsets) {
            return position.offset(facing);
        }
        return DirectionalOffsetCache.current().offset(position, facing);
    }
}
