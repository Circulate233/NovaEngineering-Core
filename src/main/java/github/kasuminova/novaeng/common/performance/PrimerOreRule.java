package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Arrays;

public final class PrimerOreRule {
    private static final int HEIGHT_CACHE_RADIUS = 8;
    private static final int HEIGHT_CACHE_CAPACITY = HEIGHT_CACHE_RADIUS * 2 + 1;
    public final String name;
    public final int count;
    public final int size;
    public final int minY;
    public final int maxY;
    public final boolean triangular;
    public final int rarity;
    public final long fingerprint;
    private final IBlockState singleOutput;
    private final IBlockState[] outputs;
    private final int[] weights;
    private final int totalWeight;
    private final ReferenceSet<IBlockState> hosts;

    public PrimerOreRule(final String name, final int count, final int size, final int minY, final int maxY,
                         final boolean triangular, final int rarity, final IBlockState[] outputs,
                         final int[] weights, final ReferenceSet<IBlockState> hosts, final long fingerprint) {
        this.name = name;
        this.count = count;
        this.size = size;
        this.minY = minY;
        this.maxY = maxY;
        this.triangular = triangular;
        this.rarity = rarity;
        this.singleOutput = outputs.length == 1 ? outputs[0] : null;
        this.outputs = outputs.length == 1 ? null : outputs;
        this.weights = outputs.length == 1 ? null : weights;
        int total = 0;
        for (final int weight : weights) total = Math.addExact(total, weight);
        this.totalWeight = total;
        this.hosts = hosts;
        this.fingerprint = fingerprint;
    }

    public PrimerOreRule(final String name, final int count, final int size, final int minY, final int maxY,
                         final boolean triangular, final int rarity, final IBlockState output, final ReferenceSet<IBlockState> hosts,
                         final long fingerprint) {
        this.name = name;
        this.count = count;
        this.size = size;
        this.minY = minY;
        this.maxY = maxY;
        this.triangular = triangular;
        this.rarity = rarity;
        this.singleOutput = output;
        this.outputs = null;
        this.weights = null;
        this.totalWeight = 1;
        this.hosts = hosts;
        this.fingerprint = fingerprint;
    }

    public static long mix(long value) {
        value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
        value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    public IBlockState output(final int index) {
        return this.outputs == null ? this.singleOutput : this.outputs[index];
    }

    public void generate(final World world, final ChunkPrimer primer, final int targetX, final int targetZ, final int sourceX,
                         final int sourceZ, final long seed, final Scratch scratch) {
        final OreGenerationRandom source = scratch.source;
        source.setSeed(seed);
        if (this.rarity > 1 && source.nextInt(this.rarity) != 0) return;
        scratch.primer = primer;
        scratch.world = world;
        scratch.rule = this;
        scratch.worldX = targetX << 4;
        scratch.worldZ = targetZ << 4;
        try {
            final int offsetX = (sourceX - targetX) * 16;
            final int offsetZ = (sourceZ - targetZ) * 16;
            final int reach = this.size < 4 ? 1 : (this.size * 3 + 15) / 16 + 2;
            for (int vein = 0; vein < this.count; vein++) {
                final int x = offsetX + source.nextInt(16);
                final int y = this.triangular
                    ? this.minY + source.nextInt(this.maxY) + source.nextInt(this.maxY) - this.maxY
                    : this.minY + (this.maxY == this.minY ? 0 : source.nextInt(this.maxY - this.minY));
                final int z = offsetZ + source.nextInt(16);
                scratch.veinSeed = source.nextLong();
                if (x + reach < 0 || x - reach > 15 || z + reach < 0 || z - reach > 15) continue;
                scratch.geometry.setSeed(scratch.veinSeed);
                scratch.prepareHeight(y);
                OreVeinGenerator.generate(x, y, z, this.size, true, scratch.geometry, scratch, scratch.scan);
            }
        } finally {
            scratch.primer = null;
            scratch.world = null;
            scratch.rule = null;
        }
    }

    public static final class Scratch implements OreVeinGenerator.Target {
        private final OreGenerationRandom source = new OreGenerationRandom();
        private final OreGenerationRandom geometry = new OreGenerationRandom();
        private final OreVeinGenerator.Scratch scan = new OreVeinGenerator.Scratch();
        private final BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        private final byte[] heightCache = new byte[HEIGHT_CACHE_CAPACITY];
        private World world;
        private ChunkPrimer primer;
        private PrimerOreRule rule;
        private long veinSeed;
        private int worldX;
        private int worldZ;
        private boolean heightCheckRequired = true;
        private int heightCacheMinY;
        private int heightCacheLength;

        private void prepareHeight(final int centerY) {
            final int lower = centerY - HEIGHT_CACHE_RADIUS;
            final int upper = centerY + HEIGHT_CACHE_RADIUS;
            this.heightCacheMinY = lower;
            this.heightCacheLength = Math.min(this.heightCache.length, upper - lower + 1);
            this.position.setPos(0, lower, 0);
            this.heightCheckRequired = this.world.isOutsideBuildHeight(this.position);
            this.position.setY(upper);
            this.heightCheckRequired |= this.world.isOutsideBuildHeight(this.position);
            if (this.heightCheckRequired) {
                Arrays.fill(this.heightCache, 0, this.heightCacheLength, (byte) 0);
            }
        }

        @Override
        public int minX() {
            return 0;
        }

        @Override
        public int maxX() {
            return 15;
        }

        @Override
        public int minZ() {
            return 0;
        }

        @Override
        public int maxZ() {
            return 15;
        }

        @Override
        public boolean place(final int x, final int y, final int z) {
            if (this.heightCheckRequired) {
                final long offset = (long) y - this.heightCacheMinY;
                if (offset >= 0 && offset < this.heightCacheLength) {
                    byte result = this.heightCache[(int) offset];
                    if (result == 0) {
                        result = (byte) (this.world.isOutsideBuildHeight(
                            this.position.setPos(this.worldX + x, y, this.worldZ + z)) ? 2 : 1);
                        this.heightCache[(int) offset] = result;
                    }
                    if (result == 2) return false;
                } else if (this.world.isOutsideBuildHeight(
                    this.position.setPos(this.worldX + x, y, this.worldZ + z))) {
                    return false;
                }
            }
            if (!this.rule.hosts.contains(this.primer.getBlockState(x, y, z))) return false;
            int selected = 0;
            final int[] weights = this.rule.weights;
            if (weights != null) {
                int weight = (int) Long.remainderUnsigned(mix(this.veinSeed
                    ^ (this.worldX + x) * 0x9E3779B97F4A7C15L ^ (this.worldZ + z) * 0xC2B2AE3D27D4EB4FL
                    ^ y * 0x165667B19E3779F9L), this.rule.totalWeight);
                while ((weight -= weights[selected]) >= 0) selected++;
            }
            this.primer.setBlockState(x, y, z, this.rule.output(selected));
            return true;
        }
    }
}
