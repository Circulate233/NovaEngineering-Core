package github.kasuminova.novaeng.common.performance;

import cofh.cofhworld.util.random.WeightedBlock;
import cofh.cofhworld.world.generator.WorldGen;
import com.google.common.base.Predicate;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class WorldOreGeneration {
    private static final int HEIGHT_CACHE_RADIUS = 8;
    private static final int HEIGHT_CACHE_CAPACITY = HEIGHT_CACHE_RADIUS * 2 + 1;
    private static final ThreadLocal<ObjectArrayList<Target>> IDLE = ThreadLocal.withInitial(() -> new ObjectArrayList<>(2));

    private WorldOreGeneration() {
    }

    public static boolean vanilla(final World world, final Random random, final BlockPos origin,
                                  final int size, final IBlockState output, final Predicate<IBlockState> predicate) {
        final Target target = borrow(world);
        target.vanilla = true;
        target.output = output;
        target.predicate = predicate;
        target.prepareHeight(origin.getY());
        try {
            OreVeinGenerator.generate(origin.getX() + 8, origin.getY(), origin.getZ() + 8,
                size, false, random, target, target.scratch);
            return true;
        } finally {
            release(target);
        }
    }

    public static boolean cofh(final World world, final Random random, final BlockPos origin, final int size,
                               final WeightedBlock[] hosts, final List<WeightedBlock> outputs) {
        final Target target = borrow(world);
        target.vanilla = false;
        target.random = random;
        target.hosts = hosts;
        target.outputs = outputs;
        target.prepareHeight(origin.getY());
        try {
            return OreVeinGenerator.generate(origin.getX(), origin.getY(), origin.getZ(), size, true,
                random, target, target.scratch);
        } finally {
            release(target);
        }
    }

    private static Target borrow(final World world) {
        final ObjectArrayList<Target> idle = IDLE.get();
        final Target target = idle.isEmpty() ? new Target() : idle.removeLast();
        target.world = world;
        return target;
    }

    private static void release(final Target target) {
        target.world = null;
        target.output = null;
        target.predicate = null;
        target.hosts = null;
        target.outputs = null;
        target.random = null;
        target.vanilla = false;
        target.heightCheckRequired = true;
        final ObjectArrayList<Target> idle = IDLE.get();
        if (idle.size() < 2) idle.add(target);
    }

    private static final class Target implements OreVeinGenerator.Target {
        private final OreVeinGenerator.Scratch scratch = new OreVeinGenerator.Scratch();
        private final BlockPos.MutableBlockPos heightProbe = new BlockPos.MutableBlockPos();
        private final byte[] heightCache = new byte[HEIGHT_CACHE_CAPACITY];
        private World world;
        private IBlockState output;
        private Predicate<IBlockState> predicate;
        private WeightedBlock[] hosts;
        private List<WeightedBlock> outputs;
        private Random random;
        private boolean vanilla;
        private boolean heightCheckRequired = true;
        private int heightCacheMinY;
        private int heightCacheLength;

        private void prepareHeight(final int centerY) {
            final int lower = centerY - HEIGHT_CACHE_RADIUS;
            final int upper = centerY + HEIGHT_CACHE_RADIUS;
            this.heightCacheMinY = lower;
            this.heightCacheLength = Math.min(this.heightCache.length, upper - lower + 1);
            this.heightProbe.setPos(0, lower, 0);
            this.heightCheckRequired = this.world.isOutsideBuildHeight(this.heightProbe);
            this.heightProbe.setY(upper);
            this.heightCheckRequired |= this.world.isOutsideBuildHeight(this.heightProbe);
            if (this.heightCheckRequired) {
                Arrays.fill(this.heightCache, 0, this.heightCacheLength, (byte) 0);
            }
        }

        @Override
        public boolean place(final int x, final int y, final int z) {
            if (this.heightCheckRequired) {
                final long offset = (long) y - this.heightCacheMinY;
                if (offset >= 0 && offset < this.heightCacheLength) {
                    byte result = this.heightCache[(int) offset];
                    if (result == 0) {
                        this.heightProbe.setPos(x, y, z);
                        result = (byte) (this.world.isOutsideBuildHeight(this.heightProbe) ? 2 : 1);
                        this.heightCache[(int) offset] = result;
                    }
                    if (result == 2) return false;
                } else if (this.world.isOutsideBuildHeight(this.heightProbe.setPos(x, y, z))) {
                    return false;
                }
            }
            final BlockPos position = this.heightProbe.setPos(x, y, z);
            if (!this.vanilla && (this.hosts == null || this.hosts.length == 0)) {
                return WorldGen.setBlock(this.world, position, WorldGen.selectBlock(this.random, this.outputs));
            }
            final IBlockState state = this.world.getBlockState(position);
            final Block block = state.getBlock();
            if (this.vanilla) {
                if (!block.isReplaceableOreGen(state, this.world, position, this.predicate)) return false;
                return WorldgenOrePlacement.set(this.world, position, this.output, 2);
            }
            int metadata = 0;
            boolean metadataResolved = false;
            for (final WeightedBlock host : this.hosts) {
                if (host.metadata != -1) {
                    if (!metadataResolved) {
                        metadata = block.getMetaFromState(state);
                        metadataResolved = true;
                    }
                    if (host.metadata != metadata) continue;
                }
                if (block.isReplaceableOreGen(state, this.world, position,
                    ((WeightedBlockPredicate) (Object) host).nova$replacementPredicate())
                    || block.isAssociatedBlock(host.block)) {
                    return WorldGen.setBlock(this.world, position, WorldGen.selectBlock(this.random, this.outputs));
                }
            }
            return false;
        }
    }
}
