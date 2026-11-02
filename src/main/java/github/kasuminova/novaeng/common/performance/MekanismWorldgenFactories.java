package github.kasuminova.novaeng.common.performance;

import com.google.common.base.Predicate;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import mekanism.common.MekanismBlocks;
import mekanism.common.world.WorldGenSalt;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.world.gen.feature.WorldGenMinable;

public final class MekanismWorldgenFactories {
    private static final ThreadLocal<Factories> NOVA_FACTORIES = ThreadLocal.withInitial(Factories::new);

    private MekanismWorldgenFactories() {
    }

    public static BlockMatcher matcher(final Block block) {
        final Factories factories = NOVA_FACTORIES.get();
        BlockMatcher matcher = factories.matchers.get(block);
        if (matcher == null) {
            if (factories.matchers.size() >= 32) factories.matchers.clear();
            matcher = BlockMatcher.forBlock(block);
            factories.matchers.put(block, matcher);
        }
        return matcher;
    }

    public static WorldGenMinable ore(final IBlockState state, final int size, final Predicate<IBlockState> predicate) {
        final Factories factories = NOVA_FACTORIES.get();
        Factory factory = factories.ores.get(state);
        if (factory == null || factory.size != size || factory.predicate != predicate) {
            if (factories.ores.size() >= 32) factories.ores.clear();
            factory = new Factory(size, predicate, new WorldGenMinable(state, size, predicate));
            factories.ores.put(state, factory);
        }
        return factory.generator;
    }

    public static WorldGenSalt salt(final int size) {
        final Factories factories = NOVA_FACTORIES.get();
        if (factories.saltBlock != MekanismBlocks.SaltBlock) {
            factories.salts.clear();
            factories.saltBlock = MekanismBlocks.SaltBlock;
        }
        final Int2ObjectOpenHashMap<WorldGenSalt> salts = factories.salts;
        WorldGenSalt generator = salts.get(size);
        if (generator == null) {
            if (salts.size() >= 16) salts.clear();
            generator = new WorldGenSalt(size);
            salts.put(size, generator);
        }
        return generator;
    }

    private record Factory(int size, Predicate<IBlockState> predicate, WorldGenMinable generator) {
    }

    private static final class Factories {
        private final Reference2ObjectOpenHashMap<Block, BlockMatcher> matchers = new Reference2ObjectOpenHashMap<>();
        private final Reference2ObjectOpenHashMap<IBlockState, Factory> ores = new Reference2ObjectOpenHashMap<>();
        private final Int2ObjectOpenHashMap<WorldGenSalt> salts = new Int2ObjectOpenHashMap<>();
        private Block saltBlock;
    }
}
