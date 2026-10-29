package github.kasuminova.novaeng.common.performance;

import net.minecraft.block.state.IBlockState;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A fixed-size memo for pure metadata transitions; changing the default state replaces the whole table.
 */
public final class BlockMetadataStates {

    private volatile Table table;

    public IBlockState get(final IBlockState base, final int variant) {
        final Table current = this.table;
        return current != null && current.base == base ? current.states.get(variant) : null;
    }

    public void put(final IBlockState base, final int variant, final IBlockState result) {
        Table current = this.table;
        if (current == null || current.base != base) {
            current = new Table(base);
            this.table = current;
        }
        current.states.set(variant, result);
    }

    private static final class Table {
        private final IBlockState base;
        private final AtomicReferenceArray<IBlockState> states = new AtomicReferenceArray<>(7);

        private Table(final IBlockState base) {
            this.base = base;
        }
    }
}
