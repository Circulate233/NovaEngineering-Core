package github.kasuminova.novaeng.common.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Preserves Konkrete's ordered replacements, with a literal path for Minecraft format prefixes.
 */
public final class FormatCodeConversion {
    private static final String CODES = "0123456789abcdefklmnor";
    private static final Object2ObjectLinkedOpenHashMap<String, Pattern> PATTERNS = new Object2ObjectLinkedOpenHashMap<>();
    private static final int MAX_PATTERNS = 128;

    private FormatCodeConversion() {
    }

    public static String convert(final String input, final String oldPrefix, final String newPrefix) {
        Objects.requireNonNull(input);
        if (oldPrefix != null && oldPrefix.length() == 1 && newPrefix != null && newPrefix.length() == 1
            && (oldPrefix.charAt(0) == '&' || oldPrefix.charAt(0) == '§')
            && newPrefix.charAt(0) != '$' && newPrefix.charAt(0) != '\\') {
            final char oldCode = oldPrefix.charAt(0);
            final char newCode = newPrefix.charAt(0);
            StringBuilder result = null;
            for (int index = input.indexOf(oldCode); index >= 0 && index + 1 < input.length();
                 index = input.indexOf(oldCode, index + 1)) {
                if (CODES.indexOf(input.charAt(index + 1)) >= 0) {
                    if (result == null) result = new StringBuilder(input);
                    result.setCharAt(index, newCode);
                }
            }
            return result == null ? input : result.toString();
        }
        String result = input;
        for (int index = 0; index < CODES.length(); index++) {
            final char code = CODES.charAt(index);
            result = pattern(oldPrefix + code).matcher(result).replaceAll(newPrefix + code);
        }
        return result;
    }

    private static Pattern pattern(final String expression) {
        synchronized (PATTERNS) {
            Pattern pattern = PATTERNS.getAndMoveToLast(expression);
            if (pattern == null) {
                pattern = Pattern.compile(expression);
                if (PATTERNS.size() == MAX_PATTERNS) PATTERNS.removeFirst();
                PATTERNS.put(expression, pattern);
            }
            return pattern;
        }
    }
}
