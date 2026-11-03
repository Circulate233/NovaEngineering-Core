package github.kasuminova.novaeng.mixin.minecraft;

import github.kasuminova.novaeng.common.performance.PrimerOreRule;
import github.kasuminova.novaeng.common.performance.PrimerOreSettings;
import github.kasuminova.novaeng.common.performance.VanillaPrimerOreRules;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChunkGeneratorOverworld.class)
public abstract class MixinOverworldPrimerOres implements PrimerOreSettings {
    @Shadow
    private ChunkGeneratorSettings settings;
    @Unique
    private PrimerOreRule[] nova$primerRules;

    @Override
    public PrimerOreRule[] nova$getVanillaPrimerRules(final World world) {
        if (this.nova$primerRules == null) this.nova$primerRules = VanillaPrimerOreRules.compile(this.settings, world);
        return this.nova$primerRules;
    }
}
