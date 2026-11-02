package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.NovaEngCoreConfig;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.event.terraingen.OreGenEvent.GenerateMinable.EventType;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.fml.common.Loader;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

public final class PrimerOreGeneration {
    private static final Map<World, Plan> PLANS = new WeakHashMap<>();
    private static final ThreadLocal<Context> CONTEXT = ThreadLocal.withInitial(Context::new);

    private PrimerOreGeneration() {
    }

    public static long[] generate(final World world, final ChunkPrimer primer, final int x, final int z) {
        if (world.isRemote || !NovaEngCoreConfig.SERVER.generateOresInTerrain
            || world.getWorldType() == WorldType.DEBUG_ALL_BLOCK_STATES
            || !(world.getChunkProvider() instanceof ChunkProviderServer provider)
            || !(provider.chunkGenerator instanceof PrimerOreSettings settings)) return null;
        final Context context = CONTEXT.get();
        if (context.active) return null;
        context.active = true;
        try {
            final Plan plan = plan(world, settings);
            if (plan.rules.length == 0) return null;
            final LongArrayList completed = context.completed;
            completed.clear();
            completed.ensureCapacity(plan.rules.length);
            final long worldSeed = world.getSeed();
            BlockPos origin = null;
            for (final Entry entry : plan.rules) {
                if (entry.type != null) {
                    if (origin == null) origin = new BlockPos(x << 4, 0, z << 4);
                    context.events.setSeed(seed(worldSeed, x, z, entry.id));
                    if (!TerrainGen.generateOre(world, context.events, entry.generator, origin, entry.type)) continue;
                }
                generateRule(entry.rule, world, primer, x, z, worldSeed, entry.id, context.scratch);
                completed.add(entry.id);
            }
            if (completed.isEmpty()) return null;
            final long[] result = completed.toLongArray();
            Arrays.sort(result);
            return result;
        } finally {
            context.active = false;
        }
    }

    public static void generateRule(final PrimerOreRule rule, final World world, final ChunkPrimer primer, final int x, final int z,
                                    final long worldSeed, final long id, final PrimerOreRule.Scratch scratch) {
        for (int sourceX = x - 1; sourceX <= x + 1; sourceX++) {
            for (int sourceZ = z - 1; sourceZ <= z + 1; sourceZ++) {
                rule.generate(world, primer, x, z, sourceX, sourceZ, seed(worldSeed, sourceX, sourceZ, id), scratch);
            }
        }
    }

    public static long seed(final long worldSeed, final int x, final int z, final long id) {
        return PrimerOreRule.mix(worldSeed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ id);
    }

    private static synchronized Plan plan(final World world, final PrimerOreSettings settings) {
        Plan plan = PLANS.get(world);
        if (plan != null) return plan;
        final ObjectArrayList<Entry> rules = new ObjectArrayList<>();
        for (final PrimerOreRule rule : settings.nova$getVanillaPrimerRules(world)) {
            if (rule == null) continue;
            final EventType type = EventType.valueOf(rule.name);
            rules.add(new Entry(rule, identifier("vanilla", rule.name), type, new WorldGenMinable(rule.output(0), rule.size)));
        }
        if (Loader.isModLoaded("cofhworld")) {
            for (final PrimerOreRule rule : CofhPrimerOreCompiler.compileRules(world)) {
                rules.add(new Entry(rule, identifier("cofh", rule.name), null, null));
            }
        }
        plan = new Plan(rules.toArray(new Entry[0]));
        PLANS.put(world, plan);
        return plan;
    }

    public static boolean isGenerating() {
        return CONTEXT.get().active;
    }

    public static synchronized void invalidate() {
        PLANS.clear();
    }

    public static synchronized void unload(final World world) {
        PLANS.remove(world);
    }

    public static long identifier(final String source, final String name) {
        long hash = 0xCBF29CE484222325L;
        for (int index = 0; index < source.length(); index++) hash = (hash ^ source.charAt(index)) * 0x100000001B3L;
        hash = (hash ^ '/') * 0x100000001B3L;
        for (int index = 0; index < name.length(); index++) hash = (hash ^ name.charAt(index)) * 0x100000001B3L;
        return hash;
    }

    public static boolean wasGenerated(final World world, final int x, final int z, final long id) {
        final Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
        return chunk instanceof PrimerOreChunk data && contains(data.nova$getPrimerOreRules(), id);
    }

    public static boolean contains(final long[] rules, final long id) {
        return rules != null && Arrays.binarySearch(rules, id) >= 0;
    }

    public static PrimerOreRule vanilla(final World world, final String name, final IBlockState state, final int count, final int size,
                                        final int min, final int max, final boolean triangular) {
        if (count <= 0 || count > 1024 || size <= 0 || size > 64 || (triangular && max <= 0)) return null;
        int lower = min;
        int upper = max;
        if (!triangular) {
            if (upper < lower) {
                lower = max;
                upper = min;
            }
            if (upper == lower) {
                if (world.isOutsideBuildHeight(new BlockPos(0, lower + 1, 0))) lower--;
                else upper++;
            }
        }
        return new PrimerOreRule(name, count, size, lower, upper, triangular, 1,
            state, StoneHosts.STATES, identifier("vanilla", name));
    }

    private static final class StoneHosts {
        private static final ReferenceSet<IBlockState> STATES = create();

        private static ReferenceSet<IBlockState> create() {
            final ReferenceOpenHashSet<IBlockState> states = new ReferenceOpenHashSet<>();
            for (final IBlockState state : Blocks.STONE.getBlockState().getValidStates()) {
                if (state.getValue(BlockStone.VARIANT).isNatural()) states.add(state);
            }
            return ReferenceSets.unmodifiable(states);
        }
    }

    private record Plan(Entry[] rules) {
    }

    private record Entry(PrimerOreRule rule, long id, EventType type, WorldGenMinable generator) {
    }

    private static final class Context {
        private final PrimerOreRule.Scratch scratch = new PrimerOreRule.Scratch();
        private final Random events = new Random(0L);
        private final LongArrayList completed = new LongArrayList();
        private boolean active;
    }
}
