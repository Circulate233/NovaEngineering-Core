package github.kasuminova.novaeng.mixin.rtg;

import github.kasuminova.novaeng.common.util.ReusableSimplexDiskData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rtg.api.util.noise.ISimplexData2D;
import rtg.api.world.biome.RealisticBiomeBase;

@Mixin(value = RealisticBiomeBase.class, remap = false)
public abstract class MixinRealisticBiomeLakePressure {

    @Redirect(method = "lakePressure(Lrtg/api/world/RTGWorld;IIFFFFF)F",
        at = @At(value = "INVOKE",
            target = "Lrtg/api/util/noise/SimplexData2D;newDisk()Lrtg/api/util/noise/ISimplexData2D;"),
        require = 1)
    private ISimplexData2D nova$reuseLakePressureData() {
        return ReusableSimplexDiskData.get();
    }
}
