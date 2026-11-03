package github.kasuminova.novaeng.common.performance;

import java.util.random.RandomGenerator;

public final class OreGenerationRandom implements RandomGenerator {
    private long state;

    public void setSeed(final long seed) {
        this.state = (seed ^ 0x5DEECE66DL) & ((1L << 48) - 1);
    }

    private int next(final int bits) {
        this.state = (this.state * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (int) (this.state >>> (48 - bits));
    }

    @Override
    public int nextInt() {
        return this.next(32);
    }

    @Override
    public int nextInt(final int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        final int mask = bound - 1;
        int value = this.next(31);
        if ((bound & mask) == 0) return (int) ((bound * (long) value) >> 31);
        int result = value % bound;
        while (value - result + mask < 0) {
            value = this.next(31);
            result = value % bound;
        }
        return result;
    }

    @Override
    public long nextLong() {
        return ((long) this.next(32) << 32) + this.next(32);
    }

    @Override
    public float nextFloat() {
        return this.next(24) * 0x1.0p-24F;
    }

    @Override
    public double nextDouble() {
        return (((long) this.next(26) << 27) + this.next(27)) * 0x1.0p-53;
    }
}
