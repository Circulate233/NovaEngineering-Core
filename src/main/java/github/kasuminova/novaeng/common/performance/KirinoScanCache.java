package github.kasuminova.novaeng.common.performance;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One hand-off between component and struct metadata. The scanner closes its archives before returning.
 */
public final class KirinoScanCache {
    private static final String COMPONENT = "com.cleanroommc.kirino.ecs.component.scan.CleanComponentSignature";
    private static final String STRUCT = "com.cleanroommc.kirino.ecs.component.scan.CleanStructSignature";
    private static Pending pending;

    private KirinoScanCache() {
    }

    public static synchronized Map<String, ClassInfo> scan(final List<String> packages, final String annotation) {
        if (packages.isEmpty()) {
            return new Object2ObjectRBTreeMap<>();
        }
        final ClassGraph graph = new ClassGraph().enableClassInfo().ignoreClassVisibility()
                                                 .enableAnnotationInfo().enableFieldInfo().ignoreFieldVisibility()
                                                 .acceptPackages(packages.toArray(String[]::new));
        return scanConfigured(packages, annotation, graph);
    }

    static synchronized Map<String, ClassInfo> scanConfigured(final List<String> packages, final String annotation,
                                                             final ClassGraph graph) {
        if (pending != null && annotation.equals(pending.annotation()) && packages.equals(pending.key().packages())
            && Thread.currentThread().getContextClassLoader() == pending.key().context()) {
            final Key key = currentKey(packages, graph);
            if (pending.key().equals(key)) {
                final Map<String, ClassInfo> result = pending.classes();
                pending = null;
                return result;
            }
        }
        pending = null;
        final Map<String, Archive> observed = new ConcurrentHashMap<>();
        // Observe raw entries before ClassGraph opens them. Never filter or replace ClassGraph's resolution order.
        graph.filterClasspathElements(path -> {
            try {
                final Archive archive = archive(Path.of(path));
                if (archive != null) observed.putIfAbsent(archive.path(), archive);
            } catch (IOException | InvalidPathException | SecurityException unavailable) {
                // Nested/virtual/system entries without a pre-read fingerprint simply disable the paired hand-off.
            }
            return true;
        });
        try (ScanResult result = graph.scan()) {
            final Map<String, ClassInfo> classes = selected(result, annotation);
            final Key key = key(packages, result.getClasspathFiles());
            if (key != null && (COMPONENT.equals(annotation) || STRUCT.equals(annotation))) {
                final boolean unchanged = key.archives().stream().allMatch(archive -> archive.equals(observed.get(archive.path())));
                if (unchanged) {
                    final String next = COMPONENT.equals(annotation) ? STRUCT : COMPONENT;
                    pending = new Pending(key, next, selected(result, next));
                }
            }
            return classes;
        }
    }

    private static Map<String, ClassInfo> selected(final ScanResult result, final String annotation) {
        final Map<String, ClassInfo> classes = new Object2ObjectRBTreeMap<>();
        for (final ClassInfo info : result.getClassesWithAnnotation(annotation)) {
            classes.put(info.getName(), info);
        }
        return classes;
    }

    private static Key currentKey(final List<String> packages, final ClassGraph graph) {
        try {
            return key(packages, graph.getClasspathFiles());
        } catch (SecurityException unavailable) {
            return null; // Preserve the real scan's diagnostics when the optional validation is unavailable.
        }
    }

    private static Key key(final List<String> packages, final List<File> files) {
        try {
            final List<Archive> archives = new ObjectArrayList<>();
            for (final File file : files) {
                final Archive archive = archive(file.toPath());
                if (archive == null) {
                    return null; // Mutable development directories need a fresh scan.
                }
                archives.add(archive);
            }
            return archives.isEmpty() ? null : new Key(new ObjectImmutableList<>(packages), new ObjectImmutableList<>(archives),
                Thread.currentThread().getContextClassLoader());
        } catch (IOException | SecurityException failure) {
            return null; // The real scanner retains its own diagnostics and failure behavior.
        }
    }

    private static Archive archive(final Path input) throws IOException {
        final Path path = input.toAbsolutePath().normalize();
        final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        return attrs.isRegularFile() ? new Archive(path.toString(), attrs.size(), attrs.lastModifiedTime(),
            attrs.creationTime(), String.valueOf(attrs.fileKey())) : null;
    }

    private record Archive(String path, long size, FileTime modified, FileTime created, String fileKey) {
    }

    private record Key(List<String> packages, List<Archive> archives, ClassLoader context) {
    }

    private record Pending(Key key, String annotation, Map<String, ClassInfo> classes) {
    }
}
