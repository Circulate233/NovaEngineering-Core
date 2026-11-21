package github.kasuminova.novaeng.common.performance;

import github.kasuminova.novaeng.common.crafttweaker.util.ScriptDirective;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;

/**
 * Standalone differential check: run with javac/java, without starting Minecraft or Gradle.
 */
public final class CraftTweakerLoadingCompatibilityTest {
    private static int checks;

    static void main(final String[] args) throws Exception {
        final String[] edges = {"", "#", "# ", " #loader crafttweaker  ", "# loader", "#loader\tfoo",
            "\u0000#debug\r", "\u2003#debug", "=", "==", "=value", "key=", "key=value==",
            "key=a=b", "#comment=value", "key=%2$d", "key=值", "key= ="};
        for (final String line : edges) check(line);
        final Random random = new Random(0x435453);
        final char[] alphabet = {'#', '=', ' ', '\t', '\r', '\u0000', '\u2003', 'a', 'b', '值'};
        for (int sample = 0; sample < 100_000; sample++) {
            final StringBuilder line = new StringBuilder();
            for (int i = random.nextInt(64); i > 0; i--) line.append(alphabet[random.nextInt(alphabet.length)]);
            check(line.toString());
        }
        int scripts = 0;
        try (var paths = Files.walk(Path.of(args[0]))) {
            for (final Path path : paths.filter(p -> p.toString().endsWith(".zs")).toList()) {
                for (final String line : Files.readAllLines(path, StandardCharsets.UTF_8)) check(line);
                scripts++;
            }
        }
        System.out.println("Passed " + checks + " differential cases; " + scripts + " installed scripts.");
    }

    private static void check(final String line) {
        final String trimmed = line.trim();
        String expected = null;
        if (!trimmed.isEmpty() && trimmed.charAt(0) == '#') {
            final String[] tokens = trimmed.substring(1).split(" ");
            if (tokens.length != 0) expected = tokens[0];
        }
        if (!Objects.equals(expected, ScriptDirective.name(line))) {
            throw new AssertionError("Directive mismatch: " + line);
        }
        checks++;
    }
}
