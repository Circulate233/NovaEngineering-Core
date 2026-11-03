package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.Locale;

public final class RootLowercaseCache {
    private static final ThreadLocal<Reference2ObjectOpenHashMap<String, String>> CACHE =
        ThreadLocal.withInitial(Reference2ObjectOpenHashMap::new);

    private RootLowercaseCache() {
    }

    public static String lower(final String value, final Locale locale) {
        if (locale != Locale.ROOT) return value.toLowerCase(locale);
        final Reference2ObjectOpenHashMap<String, String> cache = CACHE.get();
        String result = cache.get(value);
        if (result == null) {
            result = value.toLowerCase(locale);
            if (cache.size() >= 512) cache.clear();
            cache.put(value, result);
        }
        return result;
    }
}
