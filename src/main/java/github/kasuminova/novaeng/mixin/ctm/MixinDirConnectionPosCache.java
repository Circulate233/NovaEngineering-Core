package github.kasuminova.novaeng.mixin.ctm;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import github.kasuminova.novaeng.client.render.DirectionalOffsetCache;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.chisel.ctm.client.util.Dir;

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
