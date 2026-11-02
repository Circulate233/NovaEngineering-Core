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
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

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
        final Key key = key(packages, graph);
        if (key != null && pending != null && key.equals(pending.key())
            && annotation.equals(pending.annotation())) {
            final Map<String, ClassInfo> result = pending.classes();
            pending = null;
            return result;
        }
        pending = null;
        try (ScanResult result = graph.scan()) {
            final Map<String, ClassInfo> classes = selected(result, annotation);
            if (key != null && (COMPONENT.equals(annotation) || STRUCT.equals(annotation))) {
                final String next = COMPONENT.equals(annotation) ? STRUCT : COMPONENT;
                pending = new Pending(key, next, selected(result, next));
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

    private static Key key(final List<String> packages, final ClassGraph graph) {
        try {
            final List<Archive> archives = new ObjectArrayList<>();
            for (final File file : graph.getClasspathFiles()) {
                final var path = file.toPath().toAbsolutePath().normalize();
                final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                if (!attrs.isRegularFile()) {
                    return null; // Mutable development directories need a fresh scan.
                }
                archives.add(new Archive(path.toString(), attrs.size(), attrs.lastModifiedTime().toString(),
                    attrs.creationTime().toString(), String.valueOf(attrs.fileKey())));
            }
            return archives.isEmpty() ? null : new Key(new ObjectImmutableList<>(packages), new ObjectImmutableList<>(archives),
                Thread.currentThread().getContextClassLoader());
        } catch (IOException | SecurityException failure) {
            return null; // The real scanner retains its own diagnostics and failure behavior.
        }
    }

    private record Archive(String path, long size, String modified, String created, String fileKey) {
    }

    private record Key(List<String> packages, List<Archive> archives, ClassLoader context) {
    }

    private record Pending(Key key, String annotation, Map<String, ClassInfo> classes) {
    }
}
