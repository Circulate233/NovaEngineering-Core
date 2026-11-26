package github.kasuminova.novaeng.client.font;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** One bounded snapshot of the string-only configuration list; no mutable config list is retained. */
public final class TooltipBindings {
    private static final int MAX_ENTRIES = 256;
    private static final int MAX_CHARACTERS = 32768;
    private Object[] previous;
    private List<String> normalized;

    public synchronized List<String> resolve(final Object raw, final List<String> defaults) {
        if (!(raw instanceof List<?> values)) {
            clear();
            return defaults;
        }
        // TOML lists are ArrayLists. Unknown list implementations and arbitrary objects keep
        // the original iteration and twice-per-entry toString semantics, without memoization.
        if (values.getClass() != ArrayList.class || values.size() > MAX_ENTRIES) {
            clear();
            return normalize(values, defaults);
        }
        int characters = 0;
        boolean same = previous != null && previous.length == values.size();
        for (int i = 0; i < values.size(); i++) {
            final Object value = values.get(i);
            if (value != null && !(value instanceof String)) {
                clear();
                return normalize(values, defaults);
            }
            if (value != null) characters += ((String) value).length();
            if (characters > MAX_CHARACTERS) {
                clear();
                return normalize(values, defaults);
            }
            if (same && !Objects.equals(previous[i], value)) same = false;
        }
        if (!same) {
            previous = values.toArray();
            normalized = normalize(values, Collections.emptyList());
        }
        return normalized.isEmpty() ? defaults : normalized;
    }

    private void clear() {
        previous = null;
        normalized = null;
    }

    private static List<String> normalize(final List<?> values, final List<String> defaults) {
        final ArrayList<String> result = new ArrayList<>();
        for (final Object value : values) {
            if (value != null && !value.toString().trim().isEmpty()) result.add(value.toString());
        }
        return result.isEmpty() ? defaults : Collections.unmodifiableList(result);
    }
}
