package github.kasuminova.novaeng.common.performance;

import cofh.cofhworld.data.numbers.ConstantProvider;
import cofh.cofhworld.data.numbers.INumberProvider;
import cofh.cofhworld.init.WorldHandler;
import cofh.cofhworld.init.WorldProps;
import cofh.cofhworld.util.random.WeightedBlock;
import cofh.cofhworld.world.IConfigurableFeatureGenerator.GenRestriction;
import cofh.cofhworld.world.distribution.Distribution;
import cofh.cofhworld.world.generator.WorldGenMinableCluster;
import github.kasuminova.novaeng.mixin.cofh.AccessorPrimerOreCluster;
import github.kasuminova.novaeng.mixin.cofh.AccessorPrimerOreDistribution;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import java.util.List;

public final class CofhPrimerOreCompiler {

    private CofhPrimerOreCompiler() {
    }

    /** Only called after CoFH World has been confirmed present. */
    public static List<PrimerOreRule> compileRules(final World world) {
        if (WorldProps.disableFeatureGeneration) return ObjectLists.emptyList();
        final var features = WorldHandler.getFeatures();
        if (features == null) return ObjectLists.emptyList();
        final ObjectArrayList<PrimerOreRule> rules = new ObjectArrayList<>();
        for (final var feature : features) {
            if (feature instanceof PrimerOreFeature compilable) {
                final PrimerOreRule rule = compilable.nova$compilePrimerOre(world);
                if (rule != null) rules.add(rule);
            }
        }
        return rules;
    }

    public static PrimerOreRule compile(final Distribution distribution, final World world, final WorldGenerator generator,
                                        final INumberProvider count, final INumberProvider minY, final INumberProvider maxY,
                                        final boolean triangular) {
        if (!(generator instanceof WorldGenMinableCluster)) return null;
        final AccessorPrimerOreDistribution restrictions = (AccessorPrimerOreDistribution) distribution;
        if (!restrictions.nova$getWithVillage() || restrictions.nova$getBiomeRestriction() != GenRestriction.NONE)
            return null;
        final GenRestriction dimension = restrictions.nova$getDimensionRestriction();
        if (dimension != GenRestriction.NONE && (dimension == GenRestriction.BLACKLIST)
            == restrictions.nova$getDimensions().contains(world.provider.getDimension())) return null;
        final AccessorPrimerOreCluster cluster = (AccessorPrimerOreCluster) generator;
        final int attempts = constant(count);
        final int size = constant(cluster.nova$getClusterSize());
        final int lower = constant(minY);
        final int upper = constant(maxY);
        if (attempts < 0 || attempts > 1024 || size < 1
            || minY == null || minY.getClass() != ConstantProvider.class
            || maxY == null || maxY.getClass() != ConstantProvider.class
            || (!triangular && upper < lower) || (triangular && upper <= 0)) return null;
        final List<WeightedBlock> outputs = cluster.nova$getCluster();
        if (outputs.isEmpty()) return null;
        final int outputCount = outputs.size();
        final IBlockState[] states = outputCount == 1 ? null : new IBlockState[outputCount];
        final int[] weights = outputCount == 1 ? null : new int[outputCount];
        IBlockState single = null;
        long fingerprint = hash(distribution.getFeatureName());
        long total = 0;
        for (int index = 0; index < outputCount; index++) {
            final WeightedBlock output = outputs.get(index);
            final IBlockState state = output.getState();
            if (output.data != null || output.itemWeight <= 0 || state.getMaterial().isLiquid()
                || state.getBlock().hasTileEntity(state) || state.getLightValue() != 0) return null;
            if (outputCount == 1) single = state;
            else {
                states[index] = state;
                weights[index] = output.itemWeight;
            }
            total += output.itemWeight;
            if (total > Integer.MAX_VALUE) return null;
            fingerprint = PrimerOreRule.mix(fingerprint ^ hash(state.toString()) ^ output.itemWeight);
        }
        final ReferenceOpenHashSet<IBlockState> hosts = new ReferenceOpenHashSet<>();
        for (final WeightedBlock host : cluster.nova$getMaterials()) {
            final Block block = host.block;
            if (block == null) return null;
            for (final IBlockState state : block.getBlockState().getValidStates()) {
                if (host.metadata == -1 || host.metadata == block.getMetaFromState(state)) hosts.add(state);
            }
            fingerprint = PrimerOreRule.mix(fingerprint ^ hash(String.valueOf(block.getRegistryName())) ^ host.metadata);
        }
        if (hosts.isEmpty()) return null;
        fingerprint = PrimerOreRule.mix(fingerprint ^ attempts ^ ((long) Math.min(size, 42) << 16)
            ^ ((long) lower << 24) ^ ((long) upper << 40) ^ restrictions.nova$getRarity());
        return outputCount == 1
            ? new PrimerOreRule(distribution.getFeatureName(), attempts, Math.min(size, 42), lower, upper,
            triangular, restrictions.nova$getRarity(), single, hosts, fingerprint)
            : new PrimerOreRule(distribution.getFeatureName(), attempts, Math.min(size, 42), lower, upper,
            triangular, restrictions.nova$getRarity(), states, weights, hosts, fingerprint);
    }

    public static int constant(final INumberProvider value) {
        return value != null && value.getClass() == ConstantProvider.class ? value.intValue(null, null, null) : -1;
    }

    public static long hash(final String value) {
        long hash = 0xCBF29CE484222325L;
        for (int index = 0; index < value.length(); index++) hash = (hash ^ value.charAt(index)) * 0x100000001B3L;
        return hash;
    }
}
