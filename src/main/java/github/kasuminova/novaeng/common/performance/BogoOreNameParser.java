package github.kasuminova.novaeng.common.performance;

import java.util.function.Predicate;

public final class BogoOreNameParser {
    private BogoOreNameParser() {
    }

    /**
     * Returns the last matching token prefix, preserving BogoSorter's char-based splitting.
     */
    public static String findPrefix(final String name, final Predicate<String> configured) {
        if (configured.test(name)) return null;
        String match = null;
        int tokenStart = 0;
        for (int i = 0; i < name.length(); i++) {
            if (!Character.isUpperCase(name.charAt(i))) continue;
            // An uppercase char with an empty builder is emitted on its own by the original.
            final int end = i > tokenStart ? i : i + 1;
            tokenStart = end;
            if (end == name.length()) continue; // The whole name was already checked above.
            final String prefix = name.substring(0, end);
            if (configured.test(prefix)) match = prefix;
        }
        return match;
    }
}
