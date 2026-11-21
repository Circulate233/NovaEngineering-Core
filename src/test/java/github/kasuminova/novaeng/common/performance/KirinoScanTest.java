package github.kasuminova.novaeng.common.performance;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class KirinoScanTest {
    private static final String COMPONENT = "com.cleanroommc.kirino.ecs.component.scan.CleanComponentSignature";
    private static final String STRUCT = "com.cleanroommc.kirino.ecs.component.scan.CleanStructSignature";
    @TempDir Path directory;

    @Test
    void pairedScanUsesOneFullScanAndStillDetectsArchiveChanges() throws Exception {
        final Path jar = directory.resolve("components.jar");
        write(jar, false);
        final CountingGraph first = graph(jar);
        assertEquals(Set.of("test.scan.Component"), KirinoScanCache.scanConfigured(List.of("test.scan"), COMPONENT, first).keySet());
        assertEquals(1, first.scans);
        assertEquals(0, first.classpathScans, "The full scan must not be preceded by a classpath-only scan");
        final CountingGraph second = graph(jar);
        assertEquals(Set.of("test.scan.Struct"), KirinoScanCache.scanConfigured(List.of("test.scan"), STRUCT, second).keySet());
        assertEquals(0, second.scans);
        assertEquals(1, second.classpathScans, "A hand-off still validates the current resolved classpath");
        KirinoScanCache.scanConfigured(List.of("test.scan"), COMPONENT, graph(jar));
        write(jar, true);
        final CountingGraph changed = graph(jar);
        assertEquals(Set.of("test.scan.Struct", "test.scan.Added"), KirinoScanCache.scanConfigured(List.of("test.scan"), STRUCT, changed).keySet());
        assertEquals(1, changed.scans);
    }

    @Test
    void newClasspathAndDevelopmentDirectoriesAlwaysRescan() throws Exception {
        final Path original = directory.resolve("original.jar");
        final Path added = directory.resolve("added.jar");
        write(original, false);
        write(added, true);
        KirinoScanCache.scanConfigured(List.of("test.scan"), COMPONENT, graph(original));
        final CountingGraph changed = graph(original);
        changed.overrideClasspath(original, added);
        assertEquals(Set.of("test.scan.Struct", "test.scan.Added"),
            KirinoScanCache.scanConfigured(List.of("test.scan"), STRUCT, changed).keySet());
        assertEquals(1, changed.scans);

        final Path classes = directory.resolve("classes");
        final Path packagePath = classes.resolve("test/scan");
        Files.createDirectories(packagePath);
        Files.write(packagePath.resolve("Struct.class"), sample("test/scan/Struct", STRUCT));
        KirinoScanCache.scanConfigured(List.of("test.scan"), COMPONENT, graph(classes));
        Files.write(packagePath.resolve("Added.class"), sample("test/scan/Added", STRUCT));
        final CountingGraph development = graph(classes);
        assertEquals(Set.of("test.scan.Struct", "test.scan.Added"),
            KirinoScanCache.scanConfigured(List.of("test.scan"), STRUCT, development).keySet());
        assertEquals(1, development.scans);
        assertEquals(0, development.classpathScans);
    }

    @Test
    void changedDuringScanOrUnavailableValidationCannotReuseMetadata() throws Exception {
        final Path jar = directory.resolve("changing.jar");
        write(jar, false);
        final CountingGraph during = graph(jar);
        during.afterScan = () -> {
            try { write(jar, true); } catch (Exception error) { throw new AssertionError(error); }
        };
        KirinoScanCache.scanConfigured(List.of("test.scan"), COMPONENT, during);
        final CountingGraph after = graph(jar);
        assertEquals(Set.of("test.scan.Struct", "test.scan.Added"),
            KirinoScanCache.scanConfigured(List.of("test.scan"), STRUCT, after).keySet());
        assertEquals(1, after.scans);
        assertEquals(0, after.classpathScans, "A changed scan must not leave a hand-off");

        // Consume the reverse hand-off, then create a new STRUCT hand-off for validation failure.
        KirinoScanCache.scanConfigured(List.of("test.scan"), COMPONENT, graph(jar));
        KirinoScanCache.scanConfigured(List.of("test.scan"), COMPONENT, graph(jar));
        final CountingGraph denied = graph(jar);
        denied.denyValidation = true;
        assertEquals(Set.of("test.scan.Struct", "test.scan.Added"),
            KirinoScanCache.scanConfigured(List.of("test.scan"), STRUCT, denied).keySet());
        assertEquals(1, denied.scans);
        assertEquals(1, denied.classpathScans);
    }

    private static CountingGraph graph(final Path path) {
        final CountingGraph graph = new CountingGraph();
        graph.overrideClasspath(path.toString()).enableClassInfo().ignoreClassVisibility()
            .enableAnnotationInfo().enableFieldInfo().ignoreFieldVisibility().acceptPackages("test.scan");
        return graph;
    }

    private static void write(final Path file, final boolean extra) throws Exception {
        try (final JarOutputStream jar = new JarOutputStream(Files.newOutputStream(file))) {
            entry(jar, "test/scan/Component", COMPONENT);
            entry(jar, "test/scan/Struct", STRUCT);
            if (extra) entry(jar, "test/scan/Added", STRUCT);
        }
    }

    private static void entry(final JarOutputStream jar, final String name, final String annotation) throws Exception {
        jar.putNextEntry(new JarEntry(name + ".class"));
        jar.write(sample(name, annotation));
        jar.closeEntry();
    }

    private static byte[] sample(final String name, final String annotation) {
        final ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        writer.visitAnnotation("L" + annotation.replace('.', '/') + ";", true).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static final class CountingGraph extends ClassGraph {
        int scans;
        int classpathScans;
        Runnable afterScan;
        boolean denyValidation;
        @Override public ScanResult scan() {
            scans++;
            final ScanResult result = super.scan(2);
            if (afterScan != null) afterScan.run();
            return result;
        }
        @Override public List<File> getClasspathFiles() {
            classpathScans++;
            if (denyValidation) throw new SecurityException("test validation denied");
            return super.getClasspathFiles();
        }
    }
}
