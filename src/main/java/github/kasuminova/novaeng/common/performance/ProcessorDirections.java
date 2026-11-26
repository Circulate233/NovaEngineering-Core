package github.kasuminova.novaeng.common.performance;

import net.minecraft.util.EnumFacing;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

/** Six-direction snapshots; each caller retains independent, fully mutable List semantics. */
public final class ProcessorDirections {
    private static final List<List<EnumFacing>> DIRECTIONS = createDirections();

    private ProcessorDirections() {
    }

    public static List<EnumFacing> snapshot(final Object[] handlers) {
        int mask = 0;
        // Match IProcessor.autoPush's six-element scan, including malformed short-array failures.
        for (int i = 0; i < 6; i++) if (handlers[i] != null) mask |= 1 << i;
        return new MutableSnapshot(DIRECTIONS.get(mask));
    }

    private static List<List<EnumFacing>> createDirections() {
        final EnumFacing[] facing = EnumFacing.values();
        final List<List<EnumFacing>> result = new ArrayList<>(64);
        for (int mask = 0; mask < 64; mask++) {
            final List<EnumFacing> values = new ArrayList<>(Integer.bitCount(mask));
            for (int i = 0; i < 6; i++) if ((mask & (1 << i)) != 0) values.add(facing[i]);
            result.add(List.copyOf(values));
        }
        return List.copyOf(result);
    }

    private static final class MutableSnapshot extends AbstractList<EnumFacing> implements RandomAccess {
        private List<EnumFacing> values;
        private boolean copied;

        private MutableSnapshot(final List<EnumFacing> values) {
            this.values = values;
        }

        @Override public EnumFacing get(final int index) { return values.get(index); }
        @Override public int size() { return values.size(); }

        private void copy() {
            if (!copied) {
                values = new ArrayList<>(values);
                copied = true;
            }
        }

        @Override public EnumFacing set(final int index, final EnumFacing value) {
            copy();
            return values.set(index, value);
        }

        @Override public void add(final int index, final EnumFacing value) {
            copy();
            values.add(index, value);
            modCount++;
        }

        @Override public EnumFacing remove(final int index) {
            copy();
            final EnumFacing previous = values.remove(index);
            modCount++;
            return previous;
        }
    }
}
