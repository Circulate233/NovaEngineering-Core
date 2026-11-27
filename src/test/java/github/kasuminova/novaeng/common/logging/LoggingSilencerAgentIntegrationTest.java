package github.kasuminova.novaeng.common.logging;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Forks real JVMs so Instrumentation never silences the test runner's own diagnostics. */
class LoggingSilencerAgentIntegrationTest {
    private static final String AGENT = "github.kasuminova.novaeng.common.logging.agent.LoggingSilencerAgent";
    private static final String AGENT_PACKAGE = "github/kasuminova/novaeng/common/logging/agent/";

    @TempDir
    Path temporary;

    @Test
    void missingConfigurationLeavesExistingLoggingEnabled() throws Exception {
        runProbe(false, true);
    }

    @Test
    void enabledConfigurationSilencesAlreadyLoadedAndNewLoggerEntrypoints() throws Exception {
        runProbe(true, true);
    }

    @Test
    void enabledConfigurationSilencesLoggerClassesOnTheirFirstLoad() throws Exception {
        runProbe(true, false);
    }

    @Test
    void builtAgentContainsOnlyItsIsolatedClassesAndAgentManifest() throws Exception {
        try (final JarFile jar = new JarFile(agentJar().toFile())) {
            final var attributes = jar.getManifest().getMainAttributes();
            assertEquals(AGENT, attributes.getValue("Premain-Class"));
            assertEquals("true", attributes.getValue("Can-Retransform-Classes"));
            assertNull(attributes.getValue(Attributes.Name.CLASS_PATH));
            assertNull(attributes.getValue("FMLCorePlugin"));
            assertNull(attributes.getValue("MixinConfigs"));
            final var classes = jar.stream().filter(entry -> entry.getName().endsWith(".class")).toList();
            assertTrue(classes.stream().anyMatch(entry -> entry.getName().equals(AGENT.replace('.', '/') + ".class")));
            assertTrue(classes.stream().allMatch(entry -> entry.getName().startsWith(AGENT_PACKAGE)),
                "The JVM agent must not publish game or mod classes into the system classloader");
            assertTrue(jar.stream().noneMatch(entry -> entry.getName().endsWith(".jar")),
                "The standalone agent needs no bundled library JARs");
        }
    }

    private static String classpath() throws Exception {
        final ObjectLinkedOpenHashSet<String> paths = new ObjectLinkedOpenHashSet<>();
        for (ClassLoader loader = LoggingSilencerAgentIntegrationTest.class.getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader instanceof URLClassLoader urls) {
                for (final URL url : urls.getURLs()) {
                    if (url.getProtocol().equals("file")) paths.add(Path.of(url.toURI()).toAbsolutePath().toString());
                }
            }
        }
        for (final String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            paths.add(Path.of(entry).toAbsolutePath().toString());
        }
        return String.join(File.pathSeparator, paths);
    }

    private static Path agentJar() {
        final String configured = System.getProperty("novaeng.test.loggingAgentJar");
        assertNotNull(configured, "The test task must provide the production loggingAgentJar artifact");
        final Path artifact = Path.of(configured).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(artifact), "Missing production logging agent: " + artifact);
        return artifact;
    }

    private static Path createPreloadJar(final Path destination) throws Exception {
        final String type = LoggingAgentProbe.PreloadAgent.class.getName();
        final String resource = type.replace('.', '/') + ".class";
        try (final JarOutputStream jar = new JarOutputStream(Files.newOutputStream(destination), manifest(type));
             final var input = LoggingSilencerAgentIntegrationTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing preloading agent " + resource);
            jar.putNextEntry(new JarEntry(resource));
            input.transferTo(jar);
            jar.closeEntry();
        }
        return destination;
    }

    private static Manifest manifest(final String agent) {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Premain-Class", agent);
        return manifest;
    }

    private void runProbe(final boolean enabled, final boolean preload) throws Exception {
        if (enabled) {
            final Path config = temporary.resolve("config/novaeng_core.cfg");
            Files.createDirectories(config.getParent());
            Files.writeString(config, "general {\n    performance {\n        B:DisableLogOutput=true\n    }\n}\n");
        }
        final Path agentJar = agentJar();
        final List<String> arguments = new ObjectArrayList<>();
        if (preload) {
            arguments.add("-javaagent:" + createPreloadJar(temporary.resolve("preload.jar")));
        }
        arguments.add("-javaagent:" + agentJar);
        arguments.add("-cp");
        arguments.add(classpath());
        arguments.add(LoggingAgentProbe.class.getName());
        arguments.add(Boolean.toString(enabled));
        final Path argumentFile = temporary.resolve("java-arguments.txt");
        Files.writeString(argumentFile, String.join("\n", arguments.stream().map(LoggingSilencerAgentIntegrationTest::quote).toList()),
            StandardCharsets.UTF_8);
        final Path output = temporary.resolve("probe-output.txt");
        final Path java = Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java");
        final Process process = new ProcessBuilder(java.toString(), "@" + argumentFile)
            .directory(temporary.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        final boolean finished = process.waitFor(40, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
        final String result = Files.readString(output);
        assertTrue(finished, "Logging agent probe timed out:\n" + result);
        assertEquals(0, process.exitValue(), result);
        assertTrue(result.contains("DIRECT_STDOUT_ALIVE"), result);
        assertTrue(result.contains("PROBE_OK suppressed=" + enabled), result);
    }

    private static String quote(final String argument) {
        return '"' + argument.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static boolean isWindows() {
        return File.separatorChar == '\\';
    }
}
