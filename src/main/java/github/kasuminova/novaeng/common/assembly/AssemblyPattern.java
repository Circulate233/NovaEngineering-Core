package github.kasuminova.novaeng.common.assembly;

import github.kasuminova.mmce.common.util.DynamicPattern;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import hellfirepvp.modularmachinery.common.util.BlockArray.BlockInformation;
import hellfirepvp.modularmachinery.common.util.BlockArrayCache;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import it.unimi.dsi.fastutil.objects.ObjectIterators;

import java.util.Iterator;
import java.util.Map;

/**
 * Walks the MMCE templates without cloning or translating a BlockArray.
 */
final class AssemblyPattern {
    private static final int MAX_VISITS = 1_000_000;

    private final BlockPos anchor;
    private final EnumFacing facing;
    private final int dynamicSize;
    private final Iterator<DynamicPattern> patterns;
    private final Long2ObjectOpenHashMap<BlockInformation> overlay;
    private final Iterator<Map.Entry<BlockPos, BlockInformation>> base;
    long position;
    BlockInformation information;
    private Map<BlockPos, BlockInformation> repeated;
    private Map<BlockPos, BlockInformation> end;
    private Iterator<Map.Entry<BlockPos, BlockInformation>> entries;
    private Iterator<Long2ObjectMap.Entry<BlockInformation>> overrides;
    private BlockPos start;
    private BlockPos stride;
    private int repetitions;
    private int slice;
    private long x, y, z;
    private int visits;

    AssemblyPattern(final DynamicMachine machine, final EnumFacing facing, final BlockPos anchor, int dynamicSize) {
        this.anchor = anchor;
        this.facing = facing;
        final var dynamic = machine.getDynamicPatterns().values();
        for (final var pattern : dynamic) dynamicSize = Math.max(dynamicSize, pattern.getMinSize());
        this.dynamicSize = dynamicSize;
        patterns = dynamic.iterator();
        overlay = dynamic.isEmpty() ? null : new Long2ObjectOpenHashMap<>();
        base = rotated(machine.getPattern(), facing).entrySet().iterator();
    }

    AssemblyPattern(final BlockArray resolved, final BlockPos anchor) {
        this.anchor = anchor;
        this.facing = EnumFacing.NORTH;
        this.dynamicSize = 0;
        patterns = ObjectIterators.emptyIterator();
        overlay = null;
        base = resolved.getPattern().entrySet().iterator();
    }

    private static Map<BlockPos, BlockInformation> rotated(final BlockArray pattern, final EnumFacing facing) {
        return BlockArrayCache.getBlockArrayCache(pattern, facing).getPattern();
    }

    /**
     * A call consumes one cell or advances to the next template. Returns true once exhausted.
     */
    boolean step() {
        information = null;
        if (entries != null) {
            if (entries.hasNext()) {
                final var entry = entries.next();
                overlay.put(at(entry.getKey(), x, y, z), entry.getValue());
            } else {
                entries = null;
            }
            return false;
        }
        if (repeated != null) {
            if (slice < repetitions && !repeated.isEmpty()) {
                offset(slice++);
                entries = repeated.entrySet().iterator();
            } else {
                // MMCE adds the end at start + max(1, repetitions) * stride, even for zero repeats.
                if (end != null && !end.isEmpty()) {
                    offset(Math.max(1, repetitions));
                    entries = end.entrySet().iterator();
                }
                repeated = end = null;
            }
            return false;
        }
        if (patterns.hasNext()) {
            final var pattern = patterns.next();
            repetitions = Math.clamp(dynamicSize, pattern.getMinSize(), pattern.getMaxSize());
            start = pattern.getStructureSizeOffsetStart(facing);
            stride = pattern.getStructureSizeOffset(facing);
            repeated = rotated(pattern.getPattern(), facing);
            end = pattern.getPatternEnd() == null ? null : rotated(pattern.getPatternEnd(), facing);
            slice = stride.equals(BlockPos.ORIGIN) && repetitions > 0 ? repetitions - 1 : 0;
            return false;
        }
        if (base.hasNext()) {
            final var entry = base.next();
            position = at(entry.getKey(), anchor.getX(), anchor.getY(), anchor.getZ());
            if (overlay == null || !overlay.containsKey(position)) information = entry.getValue();
            return false;
        }
        if (overlay == null) return true;
        if (overrides == null) overrides = overlay.long2ObjectEntrySet().fastIterator();
        if (!overrides.hasNext()) return true;
        final var entry = overrides.next();
        position = entry.getLongKey();
        information = entry.getValue();
        return false;
    }

    private void offset(final int index) {
        x = (long) anchor.getX() + start.getX() + (long) stride.getX() * index;
        y = (long) anchor.getY() + start.getY() + (long) stride.getY() * index;
        z = (long) anchor.getZ() + start.getZ() + (long) stride.getZ() * index;
    }

    private long at(final BlockPos relative, final long offsetX, final long offsetY, final long offsetZ) {
        if (++visits > MAX_VISITS) throw new TooLarge();
        final long px = offsetX + relative.getX();
        final long py = offsetY + relative.getY();
        final long pz = offsetZ + relative.getZ();
        if (py < 0 || py >= 256) throw new OutsideHeight(py);
        if (px < -30_000_000 || px >= 30_000_000 || pz < -30_000_000 || pz >= 30_000_000) throw new OutsideWorld();
        // Same packing as BlockPos.toLong() in 1.12; the coordinates have already been checked.
        return px << 38 | py << 26 | pz & 0x3FF_FFFFL;
    }

    static final class OutsideHeight extends RuntimeException {
        final long y;

        OutsideHeight(final long y) {
            this.y = y;
        }
    }

    static final class TooLarge extends RuntimeException {
    }

    static final class OutsideWorld extends RuntimeException {
    }
}
