package github.kasuminova.novaeng.mixin.chisel;

import github.kasuminova.novaeng.common.performance.WorldgenOrePlacement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.chisel.common.util.GenerationHandler;

@Mixin(value = GenerationHandler.class, remap = false)
public abstract class MixinBasaltNotifications {
    @Redirect(method = "replace", at = @At(value = "INVOKE", remap = true,
        target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z"),
        require = 1)
    private boolean nova$placeBasalt(final World world, final BlockPos pos, final IBlockState state) {
        return WorldgenOrePlacement.setDefault(world, pos.toImmutable(), state);
    }
}
