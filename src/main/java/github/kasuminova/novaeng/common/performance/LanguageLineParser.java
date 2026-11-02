package github.kasuminova.novaeng.common.performance;

import java.util.Map;
import java.util.regex.Pattern;

public final class LanguageLineParser {
    private static final Pattern FORMATS = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");

    private LanguageLineParser() {
    }

    public static void put(final Map<String, String> properties, final String line) {
        if (line.isEmpty() || line.charAt(0) == '#') {
            return;
        }
        final int separator = line.indexOf('=');
        if (separator < 0) {
            return;
        }
        final String key = line.substring(0, separator);
        final String value = line.substring(separator + 1);
        properties.put(key, value.indexOf('%') < 0 ? value : FORMATS.matcher(value).replaceAll("%$1s"));
    }
}
