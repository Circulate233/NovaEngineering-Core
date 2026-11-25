package github.kasuminova.novaeng.mixin.pulsar;

import com.llamalad7.mixinextras.sugar.Local;
import com.sumirelabs.pulsar.light.PartialFaceLight;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PartialFaceLight.class, remap = false)
public abstract class MixinPartialFaceLight {
    @Shadow
    @Final
    private BlockPos.MutableBlockPos scratch;

    @Shadow
    private static boolean solidAt(final IBlockAccess world, final IBlockState state, final BlockPos pos,
                                   final EnumFacing normal, final double x, final double y, final double z,
                                   final boolean actualState) {
        throw new AssertionError();
    }

    // Backport Pulsar 4dbd94e: interpolate against the exposed cavity when the adjacent shape hides this vertex.
    @Redirect(method = "sampleVertex",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getCombinedLight(Lnet/minecraft/util/math/BlockPos;I)I",
            ordinal = 0, remap = true), require = 1)
    private int novaeng$sampleExposedCavity(final IBlockAccess world, final BlockPos origin, final int emission,
                                            @Local(argsOnly = true) final IBlockState rendered,
                                            @Local(argsOnly = true) final EnumFacing normal) {
        if (rendered.isOpaqueCube()) {
            this.scratch.setPos(origin.getX() + normal.getXOffset(), origin.getY() + normal.getYOffset(),
                origin.getZ() + normal.getZOffset());
            final IBlockState neighbor = world.getBlockState(this.scratch);
            if (PartialFaceLight.applies(neighbor)) {
                final EnumFacing.Axis axis = normal.getAxis();
                final double face = normal.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 0.0001 : 0.9999;
                for (int corner = 0; corner < 4; ++corner) {
                    final double u = (corner & 1) == 0 ? 0.25 : 0.75;
                    final double v = (corner & 2) == 0 ? 0.25 : 0.75;
                    final double x = axis == EnumFacing.Axis.X ? face : u;
                    final double y = axis == EnumFacing.Axis.X ? u : axis == EnumFacing.Axis.Y ? face : v;
                    final double z = axis == EnumFacing.Axis.Z ? face : v;
                    if (!solidAt(world, neighbor, this.scratch, normal, x, y, z, false)) {
                        return world.getCombinedLight(this.scratch, emission);
                    }
                }
            }
        }
        return world.getCombinedLight(origin, emission);
    }
}
