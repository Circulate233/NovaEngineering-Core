package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.block.state.IBlockState;

import java.util.List;
import java.util.Map;

public final class PulsarRgbTables {
    public static final int UNFROZEN = -2;
    public static final int MISSING = -1;
    public static final int ABSOLUTE = 1 << 12;

    private static volatile Reference2IntOpenHashMap<IBlockState> frozenColors;
    private static volatile Reference2IntOpenHashMap<IBlockState> frozenFilters;

    private PulsarRgbTables() {
    }

    // The registries hold their respective class monitors throughout mutation and publication.
    public static void freezeColors(final Reference2IntOpenHashMap<IBlockState> colors) {
        if (frozenColors == null) frozenColors = colors;
    }

    public static void freezeFilters(final Reference2IntOpenHashMap<IBlockState> filters) {
        if (frozenFilters == null) frozenFilters = filters;
    }

    public static boolean colorsFrozen() {
        return frozenColors != null;
    }

    public static boolean filtersFrozen() {
        return frozenFilters != null;
    }

    public static int color(final IBlockState state) {
        final var colors = frozenColors;
        return colors == null ? UNFROZEN : colors.getInt(state);
    }

    public static int filter(final IBlockState state) {
        final var filters = frozenFilters;
        return filters == null ? UNFROZEN : filters.getInt(state);
    }

    public static void appendColorRows(final List<String> rows, final Map<IBlockState, Integer> overrides) {
        appendRows(rows, frozenColors, overrides, false);
    }

    public static void appendFilterRows(final List<String> rows, final Map<IBlockState, Integer> overrides) {
        appendRows(rows, frozenFilters, overrides, true);
    }

    private static void appendRows(final List<String> rows, final Reference2IntOpenHashMap<IBlockState> base,
                                   final Map<IBlockState, Integer> overrides, final boolean filters) {
        final String prefix = filters ? "filter:" : "";
        final boolean hasOverrides = !overrides.isEmpty();
        final var entries = base.reference2IntEntrySet().fastIterator();
        while (entries.hasNext()) {
            final var entry = entries.next();
            final IBlockState state = entry.getKey();
            if (hasOverrides && overrides.containsKey(state)) continue;
            final int value = entry.getIntValue();
            rows.add(prefix + state + "=" + (value & 0xFFF));
            if (!filters && (value & ABSOLUTE) != 0) rows.add("absolute:" + state);
        }
        for (final var entry : overrides.entrySet()) {
            rows.add(prefix + entry.getKey() + "=" + entry.getValue().intValue());
        }
    }
}
