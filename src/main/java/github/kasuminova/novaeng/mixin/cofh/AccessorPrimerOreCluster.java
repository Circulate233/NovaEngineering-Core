package github.kasuminova.novaeng.mixin.cofh;

import cofh.cofhworld.data.numbers.INumberProvider;
import cofh.cofhworld.util.random.WeightedBlock;
import cofh.cofhworld.world.generator.WorldGenMinableCluster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = WorldGenMinableCluster.class, remap = false)
public interface AccessorPrimerOreCluster {
    @Accessor("cluster")
    List<WeightedBlock> nova$getCluster();

    @Accessor("genBlock")
    WeightedBlock[] nova$getMaterials();

    @Accessor("genClusterSize")
    INumberProvider nova$getClusterSize();
}
