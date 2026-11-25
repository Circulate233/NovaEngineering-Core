package github.kasuminova.novaeng.mixin.pulsar;

import com.llamalad7.mixinextras.sugar.Local;
import com.sumirelabs.pulsar.light.engine.LightInfo;
import com.sumirelabs.pulsar.light.engine.PulsarEngine;
import com.sumirelabs.pulsar.light.engine.ScalarBlockEngine;
import com.sumirelabs.pulsar.light.engine.ScalarSkyEngine;
import com.sumirelabs.pulsar.util.WorldHeightContext;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = {ScalarBlockEngine.class, ScalarSkyEngine.class}, remap = false)
public abstract class MixinScalarLightEdges extends PulsarEngine {
    protected MixinScalarLightEdges(final boolean sky, final World world, final WorldHeightContext height) {
        super(sky, world, height);
    }

    @Unique
    private static boolean novaeng$isOpaqueSingleSlab(final int info, final IBlockState state, final int face) {
        return face < 4 && (info & LightInfo.SIDED) != 0 && novaeng$isStaticOpaque(info)
            && state.getBlock() instanceof BlockSlab slab && !slab.isDouble();
    }

    @Unique
    private static boolean novaeng$isOppositeSlab(final IBlockState state, final IBlockState source) {
        return source.getBlock() instanceof BlockSlab slab && !slab.isDouble()
            && state.getValue(BlockSlab.HALF) != source.getValue(BlockSlab.HALF);
    }

    @Unique
    private static boolean novaeng$isStaticOpaque(final int info) {
        return (info & LightInfo.DYNAMIC) == 0 && LightInfo.opacity(info) == 15;
    }

    // Block light delegates to the WithInfo overload; sky light performs the calculation directly.
    @Redirect(method = "/^calculateLightValue(WithInfo)?$/", at = @At(value = "INVOKE",
        target = "Lcom/sumirelabs/pulsar/light/engine/LightInfo;absorption(ILnet/minecraft/block/state/IBlockState;I)I"),
        require = 1, allow = 1)
    private int novaeng$recalculateAcrossOpenFaces(final int info, final IBlockState state, final int face,
                                                   @Local(name = "offX") final int sourceX,
                                                   @Local(name = "offY") final int sourceY,
                                                   @Local(name = "offZ") final int sourceZ,
                                                   @Local(name = "neighbourLevel") final int sourceLevel,
                                                   @Local(name = "level") final int level) {
        // Even air attenuates by one. Full absorption means this sample cannot raise the result.
        if (sourceLevel <= level + 1) {
            return 15;
        }
        final int absorption = LightInfo.absorption(info, state, face);
        if (sourceLevel - absorption <= level) {
            return absorption;
        }
        final IBlockState source = this.getBlockState(sourceX, sourceY, sourceZ);
        final int sourceInfo = this.lightInfoAt(source, sourceX, sourceY, sourceZ);
        // 8b836cc: match the outgoing face mask used by the increase flood.
        if ((sourceInfo & LightInfo.REGISTRY) != 0 && LightInfo.isFaceSolid(sourceInfo, face ^ 1)) {
            return 15;
        }
        if (novaeng$isOpaqueSingleSlab(info, state, face)
            && novaeng$isOppositeSlab(state, source) && novaeng$isStaticOpaque(sourceInfo)) {
            return 15;
        }
        return absorption;
    }

    @Redirect(method = "performLightIncrease()V", at = @At(value = "INVOKE",
        target = "Lcom/sumirelabs/pulsar/light/engine/LightInfo;absorption(ILnet/minecraft/block/state/IBlockState;I)I"),
        require = 1, allow = 1)
    private int novaeng$blockSealedSlabBoundary(final int info, final IBlockState state, final int incomingFace,
                                                @Local(name = "offX") final int x,
                                                @Local(name = "offY") final int y,
                                                @Local(name = "offZ") final int z) {
        // 4dbd94e: opposite opaque slab halves close their shared horizontal face.
        if (novaeng$isOpaqueSingleSlab(info, state, incomingFace)) {
            // AxisDirection order is +X, -X, +Z, -Z, +Y, -Y. The incoming face points back to the source.
            final int step = 1 - ((incomingFace & 1) << 1);
            final int sourceX = incomingFace < 2 ? x + step : x;
            final int sourceZ = incomingFace < 2 ? z : z + step;
            final IBlockState source = this.getBlockState(sourceX, y, sourceZ);
            if (novaeng$isOppositeSlab(state, source) && novaeng$isStaticOpaque(this.lightInfoAt(source, sourceX, y, sourceZ))) {
                return 15;
            }
        }
        return LightInfo.absorption(info, state, incomingFace);
    }
}
