package github.kasuminova.novaeng.common.crafttweaker.util;

/**
 * CraftTweaker's trim/substr/split first token without copying ordinary script lines.
 */
public final class ScriptDirective {
    private ScriptDirective() {
    }

    public static String name(final String line) {
        int start = 0;
        int end = line.length();
        // String.trim(), deliberately not Unicode whitespace or tab tokenization.
        while (start < end && line.charAt(start) <= ' ') start++;
        if (start == end || line.charAt(start) != '#') return null;
        while (end > start && line.charAt(end - 1) <= ' ') end--;
        final int tokenStart = start + 1;
        final int space = line.indexOf(' ', tokenStart);
        return line.substring(tokenStart, space >= 0 && space < end ? space : end);
    }
}
