package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.data.numbers.INumberProvider;
import cofh.cofhworld.world.distribution.Distribution;
import cofh.cofhworld.world.distribution.DistributionGaussian;
import github.kasuminova.novaeng.common.performance.CofhPrimerOreCompiler;
import github.kasuminova.novaeng.common.performance.PrimerOreFeature;
import github.kasuminova.novaeng.common.performance.PrimerOreRule;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = DistributionGaussian.class, remap = false)
public abstract class MixinGaussianPrimerOres extends Distribution implements PrimerOreFeature {
    @Shadow
    @Final
    private WorldGenerator worldGen;
    @Shadow
    @Final
    private INumberProvider count;
    @Shadow
    @Final
    private INumberProvider rolls;
    @Shadow
    @Final
    private INumberProvider meanY;
    @Shadow
    @Final
    private INumberProvider maxVar;

    public MixinGaussianPrimerOres(String name, boolean regen) {
        super(name, regen);
    }

    @Override
    public PrimerOreRule nova$compilePrimerOre(final World world) {
        return CofhPrimerOreCompiler.constant(this.rolls) == 2
            ? CofhPrimerOreCompiler.compile(this, world, this.worldGen,
            this.count, this.meanY, this.maxVar, true) : null;
    }
}
