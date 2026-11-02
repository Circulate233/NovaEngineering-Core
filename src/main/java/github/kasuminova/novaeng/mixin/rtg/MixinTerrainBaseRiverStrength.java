package github.kasuminova.novaeng.mixin.rtg;

import github.kasuminova.novaeng.common.util.ReusableSimplexDiskData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rtg.api.util.noise.ISimplexData2D;
import rtg.api.world.terrain.TerrainBase;

@Mixin(value = TerrainBase.class, remap = false)
public abstract class MixinTerrainBaseRiverStrength {

    @Redirect(method = "getRiverStrength(Lnet/minecraft/util/math/BlockPos;Lrtg/api/world/RTGWorld;)F",
        at = @At(value = "INVOKE",
            target = "Lrtg/api/util/noise/SimplexData2D;newDisk()Lrtg/api/util/noise/ISimplexData2D;"),
        require = 1)
    private static ISimplexData2D nova$reuseRiverStrengthData() {
        return ReusableSimplexDiskData.get();
    }
}
