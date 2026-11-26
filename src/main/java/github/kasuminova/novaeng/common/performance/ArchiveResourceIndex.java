package github.kasuminova.novaeng.common.performance;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Negative resource probes for static archives. Unknown providers/paths always retain their original fallback.
 */
public final class ArchiveResourceIndex {
    private static final Set<String> INDEXED_ROOTS =
        ObjectSets.unmodifiable(ObjectOpenHashSet.of("recipes", "advancements", "avaritia_recipes", "cc_blockstates", "patchouli_books", "spells"));
    // Independent archive probes run concurrently; fastutil has no equivalent concurrent map.
    private static final ConcurrentMap<File, Snapshot> SNAPSHOTS = new ConcurrentHashMap<>();

    private ArchiveResourceIndex() {
    }

    public static boolean mayContain(final File source, final String requestedPath) {
        final String path = requestedPath.startsWith("/") ? requestedPath.substring(1) : requestedPath;
        if (!path.startsWith("assets/") || unusualPath(path)) {
            return true;
        }
        final int namespaceEnd = path.indexOf('/', 7);
        if (namespaceEnd < 8) {
            return true;
        }
        final int slash = path.indexOf('/', namespaceEnd + 1);
        final int rootEnd = slash < 0 ? path.length() : slash;
        if (rootEnd == namespaceEnd + 1) {
            return true;
        }
        try {
            final BasicFileAttributes attributes = Files.readAttributes(source.toPath(), BasicFileAttributes.class);
            if (!attributes.isRegularFile()) {
                return true;
            }
            Snapshot snapshot = SNAPSHOTS.get(source);
            if (snapshot == null || !snapshot.matches(attributes)) {
                snapshot = SNAPSHOTS.compute(source, (file, old) -> {
                    if (old != null && old.matches(attributes)) {
                        return old;
                    }
                    try (ZipFile zip = new ZipFile(file)) {
                        return scan(zip, attributes);
                    } catch (IOException | IllegalArgumentException failure) {
                        return null;
                    }
                });
            }
            if (snapshot == null || !snapshot.conventional) {
                return true;
            }
            final String root = path.substring(0, rootEnd);
            if (!snapshot.roots.contains(root)) {
                return false;
            }
            // The scan recorded this root from an entry, including implicit directories.
            if (path.length() == rootEnd || path.length() == rootEnd + 1 && path.charAt(rootEnd) == '/') {
                return true;
            }
            if (!INDEXED_ROOTS.contains(path.substring(namespaceEnd + 1, rootEnd))) {
                return true;
            }
            final String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            final int match = Arrays.binarySearch(snapshot.entries, normalized);
            if (match >= 0) {
                return true;
            }
            return hasDescendant(snapshot.entries, normalized);
        } catch (IOException failure) {
            return true;
        }
    }

    private static boolean hasDescendant(final String[] entries, final String path) {
        // Lower bound for the virtual key path + '/'. Do not use the insertion point for path:
        // sibling names such as "foo.json-variant" sort before "foo.json/child".
        int low = 0;
        int high = entries.length;
        while (low < high) {
            final int mid = (low + high) >>> 1;
            final String entry = entries[mid];
            final int common = Math.min(entry.length(), path.length());
            int comparison = 0;
            for (int i = 0; i < common; i++) {
                comparison = entry.charAt(i) - path.charAt(i);
                if (comparison != 0) {
                    break;
                }
            }
            if (comparison == 0) {
                comparison = entry.length() <= path.length() ? -1 : entry.charAt(path.length()) - '/';
            }
            if (comparison < 0) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low < entries.length && entries[low].startsWith(path)
            && entries[low].length() > path.length() && entries[low].charAt(path.length()) == '/';
    }

    public static boolean isConventional(final File source, final ZipFile zip) throws IOException {
        final BasicFileAttributes attributes = Files.readAttributes(source.toPath(), BasicFileAttributes.class);
        Snapshot snapshot = SNAPSHOTS.get(source);
        if (snapshot == null || !snapshot.matches(attributes)) {
            snapshot = scan(zip, attributes);
            SNAPSHOTS.put(source, snapshot);
        }
        return snapshot.conventional;
    }

    public static boolean mayContainWaypointIcons(final File source) {
        final Snapshot snapshot = SNAPSHOTS.get(source);
        if (snapshot == null || !snapshot.conventional || snapshot.waypointIcons) return true;
        try {
            return !snapshot.matches(Files.readAttributes(source.toPath(), BasicFileAttributes.class));
        } catch (IOException failure) {
            return true;
        }
    }

    private static Snapshot scan(final ZipFile zip, final BasicFileAttributes attributes) {
        final Set<String> roots = new ObjectOpenHashSet<>();
        final ObjectArrayList<String> entries = new ObjectArrayList<>();
        String previousRoot = null;
        boolean indexedRoot = false;
        boolean waypointIcons = false;
        final Enumeration<? extends ZipEntry> iterator = zip.entries();
        while (iterator.hasMoreElements()) {
            final ZipEntry entry = iterator.nextElement();
            final String name = entry.getName();
            if (unusualPath(name) || entry.getMethod() != ZipEntry.STORED && entry.getMethod() != ZipEntry.DEFLATED) {
                return snapshot(attributes, false, ObjectSets.emptySet(), new String[0], false);
            }
            if (!name.startsWith("assets/")) {
                continue;
            }
            if (previousRoot == null || !name.startsWith(previousRoot)
                || name.length() > previousRoot.length() && name.charAt(previousRoot.length()) != '/') {
                final int namespaceEnd = name.indexOf('/', 7);
                if (namespaceEnd < 8 || namespaceEnd == name.length() - 1) {
                    continue;
                }
                final int slash = name.indexOf('/', namespaceEnd + 1);
                final int rootEnd = slash < 0 ? name.length() : slash;
                if (rootEnd == namespaceEnd + 1) {
                    continue;
                }
                previousRoot = name.substring(0, rootEnd);
                roots.add(previousRoot);
                indexedRoot = INDEXED_ROOTS.contains(name.substring(namespaceEnd + 1, rootEnd));
            }
            if (!waypointIcons && previousRoot.endsWith("/textures")
                && name.startsWith("/waypoint/icon/", previousRoot.length()) && !entry.isDirectory()) {
                waypointIcons = true;
            }
            if (indexedRoot) {
                entries.add(name.endsWith("/") ? name.substring(0, name.length() - 1) : name);
            }
        }
        final String[] paths = entries.toArray(String[]::new);
        Arrays.sort(paths);
        return snapshot(attributes, true, roots, paths, waypointIcons);
    }

    private static Snapshot snapshot(final BasicFileAttributes attributes, final boolean conventional,
                                     final Set<String> roots, final String[] entries, final boolean waypointIcons) {
        return new Snapshot(attributes.size(), attributes.lastModifiedTime(), attributes.creationTime(),
            attributes.fileKey(), conventional, roots, entries, waypointIcons);
    }

    private static boolean unusualPath(final String name) {
        if (name.isEmpty() || name.charAt(0) == '/' || name.indexOf('\\') >= 0
            || name.indexOf('\0') >= 0 || name.contains("//")) {
            return true;
        }
        for (int dot = name.indexOf('.'); dot >= 0; dot = name.indexOf('.', dot + 1)) {
            if (dot == 0 || name.charAt(dot - 1) == '/') {
                int end = dot + 1;
                if (end < name.length() && name.charAt(end) == '.') {
                    end++;
                }
                if (end == name.length() || name.charAt(end) == '/') {
                    return true;
                }
            }
        }
        return false;
    }

    private record Snapshot(long size, FileTime modified, FileTime created, Object fileKey,
                            boolean conventional, Set<String> roots, String[] entries, boolean waypointIcons) {
        private boolean matches(final BasicFileAttributes attributes) {
            return size == attributes.size() && modified.equals(attributes.lastModifiedTime())
                && created.equals(attributes.creationTime()) && Objects.equals(fileKey, attributes.fileKey());
        }
    }
}
