package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.util.collection.Int2ObjectBiMap;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntIterator;

import java.util.ConcurrentModificationException;
import java.util.Map;

/**
 * Dense read-only ID lookup for a frozen registry; sparse registries retain their map.
 */
public final class RegistryIdLookup {

    private static final RegistryIdLookup DISABLED = new RegistryIdLookup(null);
    private final Object[] values;
    private final long revision;

    private RegistryIdLookup(final Object[] values) {
        this(values, -1);
    }

    private RegistryIdLookup(final Object[] values, final long revision) {
        this.values = values;
        this.revision = revision;
    }

    public static RegistryIdLookup create(final Map<Integer, ?> ids, final long revision) {
        return build(ids, revision);
    }

    public static RegistryIdLookup create(final Map<Integer, ?> ids) {
        return build(ids, -1);
    }

    private static RegistryIdLookup build(final Map<Integer, ?> ids, final long revision) {
        try {
            if (ids instanceof Int2ObjectBiMap<?> primitive) {
                if (primitive.containsKey(null))
                    return revision < 0 ? DISABLED : new RegistryIdLookup(null, revision);
                int max = -1;
                for (IntIterator iterator = primitive.keySet().iterator(); iterator.hasNext(); ) {
                    final int id = iterator.nextInt();
                    if (id < 0 || id >= 65536)
                        return revision < 0 ? DISABLED : new RegistryIdLookup(null, revision);
                    max = Math.max(max, id);
                }
                if (max >= Math.max(1024L, (long) ids.size() * 4))
                    return revision < 0 ? DISABLED : new RegistryIdLookup(null, revision);
                final Object[] values = new Object[max + 1];
                for (Int2ObjectMap.Entry<?> entry : primitive.int2ObjectEntrySet()) {
                    final int id = entry.getIntKey();
                    if (id < 0 || id >= values.length) return null;
                    values[id] = entry.getValue();
                }
                return new RegistryIdLookup(values, revision);
            }
            int max = -1;
            for (final Object key : ids.keySet()) {
                if (!(key instanceof Integer id) || id < 0 || id >= 65536) {
                    return revision < 0 ? DISABLED : new RegistryIdLookup(null, revision);
                }
                max = Math.max(max, id);
            }
            if (max >= Math.max(1024L, (long) ids.size() * 4)) {
                return revision < 0 ? DISABLED : new RegistryIdLookup(null, revision);
            }
            final Object[] values = new Object[max + 1];
            for (final Map.Entry<?, ?> entry : ids.entrySet()) {
                if (!(entry.getKey() instanceof Integer id) || id < 0 || id >= values.length) {
                    return null;
                }
                values[id] = entry.getValue();
            }
            return new RegistryIdLookup(values, revision);
        } catch (ConcurrentModificationException ignored) {
            // A freeze transition raced this build. Do not publish a partial view.
            return null;
        }
    }

    public long revision() {
        return revision;
    }

    public Object get(final int id, final Map<Integer, ?> original) {
        if (values != null) return id >= 0 && id < values.length ? values[id] : null;
        if (original instanceof Int2ObjectBiMap<?> primitive) return primitive.get(id);
        return original.get(id);
    }
}
