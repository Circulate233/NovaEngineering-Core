package github.kasuminova.novaeng.common.performance;

import java.util.ArrayList;
import java.util.List;

public final class ScriptPathSegments {
    private ScriptPathSegments() {
    }

    /** Matches String.split on dot or the platform separator, including its trailing-empty rule. */
    public static String[] split(final String path, final char separator) {
        final List<String> parts = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < path.length(); index++) {
            final char c = path.charAt(index);
            if (c == '.' || c == separator) {
                parts.add(path.substring(start, index));
                start = index + 1;
            }
        }
        if (start == 0) {
            return new String[]{path};
        }
        parts.add(path.substring(start));
        while (!parts.isEmpty() && parts.getLast().isEmpty()) {
            parts.removeLast();
        }
        return parts.toArray(String[]::new);
    }
}
