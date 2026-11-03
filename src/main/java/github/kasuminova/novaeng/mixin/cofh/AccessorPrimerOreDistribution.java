package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.world.IConfigurableFeatureGenerator.GenRestriction;
import cofh.cofhworld.world.distribution.Distribution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(value = Distribution.class, remap = false)
public interface AccessorPrimerOreDistribution {
    @Accessor("biomeRestriction")
    GenRestriction nova$getBiomeRestriction();

    @Accessor("dimensionRestriction")
    GenRestriction nova$getDimensionRestriction();

    @Accessor("dimensions")
    Set<Integer> nova$getDimensions();

    @Accessor("withVillage")
    boolean nova$getWithVillage();

    @Accessor("rarity")
    int nova$getRarity();
}
