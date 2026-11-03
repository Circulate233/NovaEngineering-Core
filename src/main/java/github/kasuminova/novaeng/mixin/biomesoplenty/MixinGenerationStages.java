package github.kasuminova.novaeng.mixin.biomesoplenty;

import biomesoplenty.api.generation.GeneratorStage;
import biomesoplenty.api.generation.IGenerator;
import biomesoplenty.common.world.GenerationManager;
import com.google.common.collect.ImmutableCollection;
import github.kasuminova.novaeng.common.performance.BopStageCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = GenerationManager.class, remap = false)
public abstract class MixinGenerationStages {
    @Unique
    private final BopStageCache nova$stages = new BopStageCache();
    @Shadow
    private Map<String, IGenerator> generators;

    @Inject(method = {"addGenerator", "removeGenerator", "configure"}, at = @At("HEAD"), require = 3)
    private void nova$invalidateStages(final CallbackInfo ci) {
        nova$stages.invalidate();
    }

    /**
     * @author NovaEngineering
     * @reason Reuse immutable stage snapshots until membership, configuration, or a standard generator's stage changes.
     */
    @Overwrite
    public ImmutableCollection<IGenerator> getGeneratorsForStage(final GeneratorStage stage) {
        return nova$stages.get(generators, stage);
    }
}
