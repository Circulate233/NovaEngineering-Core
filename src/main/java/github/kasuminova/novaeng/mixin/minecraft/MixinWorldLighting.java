package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.client.util.ClientLightingGuard;
import github.kasuminova.novaeng.client.util.LightingConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public abstract class MixinWorldLighting {

    @Shadow
    @Final
    public boolean isRemote;

    @Shadow
    public abstract int getLightFromNeighborsFor(EnumSkyBlock type, BlockPos pos);

    /**
     * @author circulation
     * @reason 高频渲染查询，覆写直接返回常量，避免注入回调对象分配
     */
    @Overwrite
    public int getCombinedLight(final BlockPos pos, final int lightValue) {
        if (this.isRemote && ClientLightingGuard.isActive()) {
            return LightingConstants.PACKED_FULL_BRIGHT;
        }
        final int sky = this.getLightFromNeighborsFor(EnumSkyBlock.SKY, pos);
        int block = this.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, pos);
        if (block < lightValue) {
            block = lightValue;
        }
        return sky << 20 | block << 4;
    }

    @Redirect(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;checkLight(Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean nova$skipLightCheck(final World world, final BlockPos pos) {
        return !world.isRemote && world.checkLight(pos);
    }

    @Redirect(method = "markBlocksDirtyVertical",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;checkLightFor(Lnet/minecraft/world/EnumSkyBlock;Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean nova$skipVerticalLightPropagation(final World world, final EnumSkyBlock type, final BlockPos pos) {
        return !world.isRemote && world.checkLightFor(type, pos);
    }
}
