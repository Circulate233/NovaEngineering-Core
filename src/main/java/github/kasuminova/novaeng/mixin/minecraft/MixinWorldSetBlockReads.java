package github.kasuminova.novaeng.mixin.minecraft;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public abstract class MixinWorldSetBlockReads {
    @Redirect(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"),
        require = 1)
    private IBlockState nova$readFromResolvedChunk(final World world, final BlockPos pos,
                                                   @Local(ordinal = 0) final Chunk chunk) {
        final Class<?> worldType = world.getClass();
        if (!world.captureBlockSnapshots && pos.getClass() == BlockPos.class
            && (worldType == WorldServer.class || worldType == WorldServerMulti.class)) {
            return chunk.getBlockState(pos);
        }
        return world.getBlockState(pos);
    }
}
