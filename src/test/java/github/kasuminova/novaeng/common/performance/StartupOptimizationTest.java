package github.kasuminova.novaeng.common.performance;

import crazypants.enderio.base.config.recipes.IRecipeConfigElement;
import crazypants.enderio.base.config.recipes.StaxFactory;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class StartupOptimizationTest {
    @Test
    void formattingMatchesMinecraftPatternAcrossAllCharacters() {
        final Pattern baseline = Pattern.compile("(?i)\u00a7[0-9A-FK-OR]");
        for (int c = 0; c <= Character.MAX_VALUE; c++) {
            final String text = "before\u00a7" + (char) c + "after\u00a7R\u00a7";
            assertEquals(baseline.matcher(text).replaceAll(""), FormattingText.strip(text));
        }
        assertEquals("", FormattingText.strip(null));
        final String plain = "No formatting";
        assertSame(plain, FormattingText.strip(plain));
    }

    @Test
    void scriptPathsMatchRegexOnWindowsAndUnix() {
        final Random random = new Random(1896);
        for (final char separator : new char[]{'/', '\\'}) {
            final String regex = "\\.|\\" + separator;
            final String alphabet = "abcXYZ .zip/\\";
            for (int i = 0; i < 10000; i++) {
                final StringBuilder path = new StringBuilder();
                for (int j = random.nextInt(70); j > 0; j--) {
                    path.append(alphabet.charAt(random.nextInt(alphabet.length())));
                }
                final String value = path.toString();
                assertArrayEquals(value.split(regex), ScriptPathSegments.split(value, separator), value);
                assertEquals(value.replaceAll("\\.zip", ""), value.replace(".zip", ""));
            }
        }
    }

    @Test
    void ingredientCopiesPreserveOrderNullsAndIndependence() {
        final Object first = new Object();
        final List<Object> group = new LinkedList<>(Arrays.asList(first, null, first));
        final List<List<Object>> groups = new LinkedList<>(List.of(List.of(), group, List.of("last")));
        final List<Object> flattened = IngredientGroups.flatten(groups);
        assertEquals(Arrays.asList(first, null, first, "last"), flattened);
        flattened.removeFirst();
        flattened.add("helper mutation");
        assertEquals(Arrays.asList(first, null, first), group);
        assertEquals(List.of(), IngredientGroups.flatten(List.of()));
    }

    @Test
    void xmlCallbacksAndFailuresMatchOriginalReader() throws Exception {
        for (final String xml : List.of(
            "<recipe a='1' b='2'> <!-- comment --> <child a='3'/><child/></recipe>",
            "<recipe bad='1'/>", "<recipe>text</recipe>", "<recipe><bad/></recipe>",
            "<recipe><?instruction data?></recipe>", "<recipe a='&amp;&lt;'><child>text</child></recipe>",
            "<recipe><child/>", "<recipe/>")) {
            assertEquals(readXml(xml, false), readXml(xml, true), xml);
        }
    }

    private static List<String> readXml(final String xml, final boolean optimized) throws Exception {
        final List<String> events = new ArrayList<>();
        final XMLEventReader reader = XMLInputFactory.newFactory().createXMLEventReader(new StringReader(xml));
        final StaxFactory factory = new StaxFactory(reader, null);
        try {
            final StartElement root = reader.nextTag().asStartElement();
            readElement(factory, reader, root, optimized, events);
        } catch (Exception failure) {
            events.add(failure.getClass().getName() + ":" + failure.getMessage());
        } finally {
            reader.close();
        }
        return events;
    }

    private static void readElement(final StaxFactory factory, final XMLEventReader reader,
                                     final StartElement start, final boolean optimized,
                                     final List<String> events) throws Exception {
        final IRecipeConfigElement target = (IRecipeConfigElement) Proxy.newProxyInstance(
            IRecipeConfigElement.class.getClassLoader(), new Class<?>[]{IRecipeConfigElement.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "setSource" -> events.add("source:" + args[0]);
                    case "setAttribute" -> {
                        events.add("attribute:" + args[1] + "=" + args[2]);
                        return !"bad".equals(args[1]);
                    }
                    case "setElement" -> {
                        events.add("element:" + args[1]);
                        if ("bad".equals(args[1])) return false;
                        readElement(factory, reader, (StartElement) args[2], optimized, events);
                        return true;
                    }
                    case "readResolve" -> events.add("resolved:" + start.getName());
                    default -> throw new AssertionError(method);
                }
                return null;
            });
        if (optimized) {
            RecipeXmlReader.read(factory, reader, null, target, start);
        } else {
            factory.read(target, start);
        }
    }

    @Test
    void jarRootUriListsRealResourcesAndKeepsExistingFileSystemOpen() throws Exception {
        final var archive = Files.createTempDirectory("nova-jar-uri-").resolve("pack with spaces.jar");
        final var normalizer = Class.forName("github.kasuminova.novaeng.mixin.vintagefix.MixinDefaultPackJarUri")
            .getDeclaredMethod("nova$completeJarRoot", String.class);
        normalizer.setAccessible(true);
        final String input = "jar:" + archive.toUri();
        final URI root = URI.create((String) normalizer.invoke(null, input));
        assertEquals(input + "!/", root.toString());
        assertEquals(root.toString(), normalizer.invoke(null, root.toString()));
        try {
            try (var fs = FileSystems.newFileSystem(root, Map.of("create", "true"))) {
                Files.createDirectories(fs.getPath("/assets/test"));
                Files.writeString(fs.getPath("/assets/test/model.json"), "{}");
                assertSame(fs, FileSystems.getFileSystem(root));
                assertTrue(Files.isRegularFile(fs.getPath("/assets/test/model.json")));
                assertTrue(fs.isOpen());
            }
        } finally {
            Files.deleteIfExists(archive);
            Files.deleteIfExists(archive.getParent());
        }
    }

    @Test
    void directoryScanPreservesFilesAndStopsOnGenerationChange() throws Exception {
        final var root = Files.createTempDirectory("nova-directory-scan-");
        final var nested = Files.createDirectories(root.resolve("assets/test/empty"));
        final var a = root.resolve("assets/test/model.json");
        final var b = root.resolve("README.txt");
        Files.writeString(a, "{}");
        Files.writeString(b, "readme");
        try {
            final Set<String> paths = new HashSet<>();
            DirectoryTreeScan.scan(root.toFile(), () -> true, paths::add);
            assertEquals(Set.of(root.relativize(a).toString(), root.relativize(b).toString()), paths);
            final AtomicBoolean current = new AtomicBoolean(true);
            paths.clear();
            DirectoryTreeScan.scan(root.toFile(), current::get, path -> { paths.add(path); current.set(false); });
            assertEquals(1, paths.size());
            paths.clear();
            DirectoryTreeScan.scan(root.toFile(), () -> false, paths::add);
            assertTrue(paths.isEmpty());
        } finally {
            Files.delete(a);
            Files.delete(b);
            Files.delete(nested);
            Files.delete(nested.getParent());
            Files.delete(nested.getParent().getParent());
            Files.delete(root);
        }
    }
}
