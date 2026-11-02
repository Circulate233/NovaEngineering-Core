package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.data.numbers.INumberProvider;
import cofh.cofhworld.world.distribution.Distribution;
import cofh.cofhworld.world.distribution.DistributionUniform;
import github.kasuminova.novaeng.common.performance.CofhPrimerOreCompiler;
import github.kasuminova.novaeng.common.performance.PrimerOreFeature;
import github.kasuminova.novaeng.common.performance.PrimerOreRule;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DistributionUniform.class, remap = false)
public abstract class MixinUniformPrimerOres implements PrimerOreFeature {
    @Shadow
    @Final
    private WorldGenerator worldGen;
    @Shadow
    @Final
    private INumberProvider count;
    @Shadow
    @Final
    private INumberProvider minY;
    @Shadow
    @Final
    private INumberProvider maxY;

    @Override
    public PrimerOreRule nova$compilePrimerOre(final World world) {
        return CofhPrimerOreCompiler.compile((Distribution) (Object) this, world, this.worldGen,
            this.count, this.minY, this.maxY, false);
    }
}
