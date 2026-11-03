package github.kasuminova.novaeng.mixin.rtg;

import github.kasuminova.novaeng.common.performance.PrimerOreRule;
import github.kasuminova.novaeng.common.performance.PrimerOreSettings;
import github.kasuminova.novaeng.common.performance.VanillaPrimerOreRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import rtg.api.world.gen.RTGChunkGenSettings;
import rtg.world.gen.ChunkGeneratorRTG;

@Mixin(value = ChunkGeneratorRTG.class, remap = false)
public abstract class MixinRtgPrimerOres implements PrimerOreSettings {
    @Shadow
    @Final
    private RTGChunkGenSettings settings;
    @Unique
    private PrimerOreRule[] nova$primerRules;

    @Override
    public PrimerOreRule[] nova$getVanillaPrimerRules(final World world) {
        if (this.nova$primerRules == null) this.nova$primerRules = VanillaPrimerOreRules.compile(this.settings, world);
        return this.nova$primerRules;
    }
}
