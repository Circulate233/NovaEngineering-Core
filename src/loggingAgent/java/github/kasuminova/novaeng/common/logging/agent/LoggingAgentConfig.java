package github.kasuminova.novaeng.common.logging.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;

/** Reads the one startup switch before Forge or a logging framework is initialized. */
public final class LoggingAgentConfig {
    private LoggingAgentConfig() {
    }

    public static boolean isEnabled(final String options) throws IOException {
        return read(configurationPath(options));
    }

    static Path configurationPath(final String options) {
        if (options == null || options.isBlank()) return Path.of("config", "novaeng_core.cfg");
        if (!options.startsWith("config=") || options.length() == "config=".length()) {
            throw new IllegalArgumentException("Expected agent option config=<path>");
        }
        return Path.of(options.substring("config=".length()));
    }

    static boolean read(final Path file) throws IOException {
        String[] categories = new String[4];
        int categoryCount = 0;
        boolean list = false;
        boolean enabled = false;
        try (final BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String raw;
            while ((raw = reader.readLine()) != null) {
                final String line = withoutComment(raw).strip();
                if (line.isEmpty()) continue;
                if (list) {
                    if (line.equals(">")) list = false;
                    continue;
                }
                if (line.equals("}")) {
                    if (categoryCount == 0) return false;
                    categories[--categoryCount] = null;
                    continue;
                }
                if (line.length() > 2 && line.charAt(1) == ':') {
                    if (line.endsWith("<")) {
                        list = true;
                        continue;
                    }
                    final int assignment = unquotedIndex(line, '=');
                    if (assignment < 0 || line.charAt(0) != 'B' || categoryCount != 2
                        || !categories[0].equals("general") || !categories[categoryCount - 1].equals("performance")) {
                        continue;
                    }
                    if (unquote(line.substring(2, assignment).strip()).equals("DisableLogOutput")) {
                        enabled = line.substring(assignment + 1).strip().equalsIgnoreCase("true");
                    }
                    continue;
                }
                if (line.endsWith("{")) {
                    final String category = unquote(line.substring(0, line.length() - 1).strip());
                    if (category.isEmpty()) return false;
                    if (categoryCount == categories.length)
                        categories = Arrays.copyOf(categories, categories.length << 1);
                    categories[categoryCount++] = category;
                }
            }
            return !list && categoryCount == 0 && enabled;
        } catch (final NoSuchFileException ignored) {
            return false;
        }
    }

    private static String withoutComment(final String line) {
        final int comment = unquotedIndex(line, '#');
        return comment < 0 ? line : line.substring(0, comment);
    }

    private static int unquotedIndex(final String line, final char delimiter) {
        boolean quoted = false;
        boolean escaped = false;
        for (int index = 0; index < line.length(); index++) {
            final char value = line.charAt(index);
            if (escaped) {
                escaped = false;
            } else if (quoted && value == '\\') {
                escaped = true;
            } else if (value == '"') {
                quoted = !quoted;
            } else if (!quoted && value == delimiter) {
                return index;
            }
        }
        return -1;
    }

    private static String unquote(final String value) {
        return value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"'
            ? value.substring(1, value.length() - 1) : value;
    }
}
