package github.kasuminova.novaeng.client.model;

import com.google.common.base.Predicate;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;

import java.util.Arrays;
import java.util.function.Supplier;

/** A small identity cache published as immutable snapshots, with an unlocked hit path. */
public final class BlockStatePredicateCache {
    private volatile Object[] entries = new Object[0];

    public Predicate<IBlockState> get(final BlockStateContainer blockState) {
        final Object[] snapshot = entries;
        for (int i = 0; i < snapshot.length; i += 2) {
            if (snapshot[i] == blockState) {
                return predicate(snapshot[i + 1]);
            }
        }
        return null;
    }

    public Predicate<IBlockState> getOrCompute(final BlockStateContainer blockState,
                                               final Supplier<Predicate<IBlockState>> factory) {
        Predicate<IBlockState> result = get(blockState);
        if (result == null) {
            synchronized (this) {
                result = get(blockState);
                if (result == null) {
                    result = factory.get();
                    final Object[] next = Arrays.copyOf(entries, entries.length + 2);
                    next[entries.length] = blockState;
                    next[entries.length + 1] = result;
                    entries = next;
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Predicate<IBlockState> predicate(final Object value) {
        return (Predicate<IBlockState>) value;
    }
}
