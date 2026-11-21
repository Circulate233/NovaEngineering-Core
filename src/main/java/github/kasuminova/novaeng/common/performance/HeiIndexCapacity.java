package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import mezz.jei.collect.MultiMap;
import mezz.jei.collect.Table;

import java.util.ArrayList;
import java.util.Map;

/**
 * Reclaims index growth slack without replacing lists, maps, keys, or their live views.
 */
public final class HeiIndexCapacity {
    private HeiIndexCapacity() {
    }

    public static void trimTable(final Table<?, ?, ?> table) {
        trimRows(((HeiTableMemoryAccess) table).nova$getRows());
    }

    public static void trimRows(final Map<?, ? extends Map<?, ?>> rows) {
        for (final Map<?, ?> row : rows.values()) {
            for (final Object value : row.values()) {
                trimList(value);
            }
            trimMap(row);
        }
        trimMap(rows);
    }

    public static void trimMultiMap(final MultiMap<?, ?, ?> map) {
        for (final var entry : map.entrySet()) {
            trimList(entry.getValue());
        }
    }

    public static void trimMap(final Map<?, ?> map) {
        if (map instanceof HeiRecipeIdMap<?> ids) {
            ids.trim();
        } else if (map instanceof Object2ObjectOpenHashMap<?, ?> objects) {
            objects.trim();
        } else if (map instanceof Reference2ObjectOpenHashMap<?, ?> references) {
            references.trim();
        } else if (map instanceof Long2ObjectOpenHashMap<?> longs) {
            longs.trim();
        } else if (map instanceof Object2LongOpenHashMap<?> values) {
            values.trim();
        }
    }

    private static void trimList(final Object value) {
        if (value instanceof ObjectArrayList<?> list) {
            list.trim();
        } else if (value instanceof ArrayList<?> list) {
            list.trimToSize();
        }
    }
}
