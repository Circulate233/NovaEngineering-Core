package github.kasuminova.novaeng.mixin.ctm;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.render.DirectionalOffsetCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.chisel.ctm.client.util.Dir;

/**
 * Stops Chisel's CTM from building the same neighbour position eight times per face.
 *
 * <p>{@code CTMLogic.buildConnectionMap} walks all eight {@code Dir} constants of the face being textured and asks
 * each of them whether the block behind it connects. Every one of those asks reaches
 * {@code Dir.applyConnection(pos, facing)}, which is {@code pos.add(getOffset(facing))} - the offset follows from the
 * face, not from the direction being asked about, so all eight pass the very same position and facing and get the
 * very same answer, each paid for with a fresh {@code BlockPos}.</p>
 *
 * <p>The measured price of that is large: while sections are being meshed, {@code BlockPos} was the largest single
 * allocation site in the whole client, around 310 MB/s and 30% of everything allocated, and the overwhelming majority
 * of it is these seven redundant answers per face. The redirect hands the computation to a per-thread one-entry memo,
 * so the repeat asks reuse the instance the first ask produced; a miss runs the original {@code pos.add(offset)}, so
 * nothing about the result changes - only how often it is computed.</p>
 */
@Mixin(value = Dir.class, remap = false)
public abstract class MixinDirConnectionPosCache {

    @Redirect(method = "applyConnection", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;add(Lnet/minecraft/util/math/Vec3i;)Lnet/minecraft/util/math/BlockPos;",
            remap = true), remap = false, require = 1)
    private BlockPos nova$reuseNeighbourPosition(final BlockPos position, final Vec3i offset) {
        if (!NovaEngCoreConfig.CLIENT.optimizeChiselCtmConnectionOffsets) {
            return position.add(offset);
        }
        return DirectionalOffsetCache.current().add(position, offset);
    }
}
